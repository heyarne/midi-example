(ns midi-example.middleware
  (:require [overtone.midi :refer [midi-handle-events]]))

;; TODO: Add tests
;; TODO: Think about how to handle non-fun-mode

(defn- current-bpm
  "Takes a list of timestamps in microseconds and returns the bpm based on the
  average interval"
  [timestamps]
  (when (> (count timestamps) 1)
    ;; get time between 4 most recent beats
    ;; build the average and convert to bpm
    (let [avg-interval (/ (->> (map - timestamps (rest timestamps))
                               (reduce +))
                          (count (rest timestamps)))]
      {:current-bpm (/ (* 60 1000 1000) avg-interval)
       :most-recent-beat (/ (first timestamps) 1000)
       :next-beat (/ (+ (first timestamps) avg-interval) 1000)})))

(defn- handle-beat
  [timestamps* event]
  (swap! timestamps* #(conj (take 3 %1) %2) (:timestamp event)))

(defn- handle-midi-clock
  [timestamps* event ppqn*]
  (condp = (:status event)
    :stop (reset! ppqn* 0)
    ;; the timing clock is sent 24 times per quarter note, so we only
    ;; take every 24th note
    :timing-clock (do
                    (when (= @ppqn* 0)
                      (handle-beat timestamps* event))
                    (swap! ppqn* #(mod (inc %) 24)))))

(defn bohemian-pulse-measurement
  "Takes a midi device and returns a function that can be used as middleware to
  assoc a :bpm key to the sketch's state containing info on current midi tempo"
  [midi-device]
  (fn [options]
    (let [timestamps (atom '())
          ppqn (atom 0) ;; pulses per quarter note; a midi clock sends 24ppqn
          old-update-fn (:update options identity)]
      ;; listen to midi events and save 4 most recent timestamps in the timestamps atom
      ;; our midi event has a channel, command, note, velocity, status and timestamp
      (midi-handle-events midi-device
                          (fn [event]
                            (condp = (:command event)
                              ;; this is just a normal tapping; we use that as a bpm count
                              :note-on (handle-beat timestamps event)
                              ;; everything else (which doesn't have a :command) is assumed to be a midi clock
                              nil (handle-midi-clock timestamps event ppqn))))
      ;; extend state to contain bpm info and pass it to the original update function
      (assoc options :update (fn [state]
                               (old-update-fn (assoc state :bpm (current-bpm @timestamps))))))))
