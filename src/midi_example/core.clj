(ns midi-example.core
  (:require [overtone.midi :refer [midi-in midi-handle-events]]
            [quil.core :as q]
            [quil.middleware :as m]
            [quil.middlewares.bind-output :as out]))

;; midi handling

(def timestamps (atom '()))

(defn on-midi-event
  [{:keys [channel command note velocity status timestamp]}]
  (when (= command :note-on)
    (swap! timestamps conj timestamp)))

(defn current-bpm
  "Takes a list of timestamps and returns the current bpm based on the last 4"
  [timestamps]
  (let [most-recent (take 4 timestamps)]
    (when (> (count most-recent) 1)
      ;; get time between 4 most recent beats
      ;; build the average and convert to bpm
      (/ (* 60 1000 1000)
         (/ (->> (map - most-recent (rest most-recent))
                 (reduce +))
            (count (rest most-recent)))))))

;; sketch drawing and setup functions

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  (q/rect-mode :center)
  (q/smooth)
  (q/text-align :center :center)
  (q/text-font (q/create-font "Helvetica" 24 true))
  ;; show midi device picker and bind an event handler
  (let [midi-device (midi-in)]
    (midi-handle-events midi-device on-midi-event)
    {}))

(defn update-state [state]
  state)

(defn draw-state [state]
  (q/background 240)
  (q/fill 40)
  (let [x (* (q/width) 0.5)
        y (* (q/height) 0.5)]
    (if-let [bpm (current-bpm @timestamps)]
      (q/text (str "Current BPM: " (int bpm)) x y)
      (q/text "Waiting for MIDI signal" x y))))

(q/defsketch midi-test
  :title "Quil MIDI Interaction"
  :size [500 500]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:keep-on-top]
  :middleware [out/bind-output m/fun-mode])
