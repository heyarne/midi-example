(ns midi-example.middleware
  (:require [overtone.midi :refer [midi-handle-events]]))

;; TODO: Make timestamps sketch-local
;; TODO: Add tests
;; TODO: Think about how to handle non-fun-mode

(defn current-bpm
  "Takes a list of timestamps in microseconds and returns the bpm based on the
  average interval"
  [timestamps]
  (when (> (count timestamps) 1)
    ;; get time between 4 most recent beats
    ;; build the average and convert to bpm
    (/ (* 60 1000 1000)
       (/ (->> (map - timestamps (rest timestamps))
               (reduce +))
          (count (rest timestamps))))))

(defn bohemian-pulse-measurement
  "Takes a midi device and returns a function that can be used as middleware to
  assoc a :bpm key to the sketch's state containing info on current midi tempo"
  [midi-device]
  (fn [options]
    (let [timestamps (atom '())
          old-update-fn (:update options identity)]
      ;; listen to midi events and save 4 most recent timestamps in the timestamps atom
      (midi-handle-events midi-device (fn [{:keys [channel command note velocity status timestamp]}]
        (when (= command :note-on)
          (swap! timestamps #(conj (take 3 %1) %2) timestamp))))
      ;; extend state to contain bpm info and pass it to the original update function
      (assoc options :update (fn [state]
                               (old-update-fn (assoc state :bpm (current-bpm @timestamps))))))))

