(ns midi-example.core
  (:require [overtone.midi :refer [midi-in]]
            [quil.core :as q]
            [quil.middleware :as m]
            ;; -> the midi-bpm handling happens in the middleware namespace
            [midi-example.middleware :as bpm]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  (q/rect-mode :center)
  (q/smooth)
  (q/text-align :center :center)
  (q/text-font (q/create-font "Helvetica" 24 true))
  {})

(defn update-state [state]
  state)

(defn draw-state [state]
  (q/background 240)
  (q/fill 40)
  (let [x (* (q/width) 0.5)
        y (* (q/height) 0.5)]
    (if-let [bpm (:bpm state)]
      (q/text (str "Current BPM: " (int bpm)) x y)
      (q/text "Waiting for MIDI signal" x y))))

;; (midi-in) opens a window which allows us to pick a midi device via GUI
(def bpm-middleware (bpm/bohemian-pulse-measurement (midi-in)))

(q/defsketch midi-test
  :title "Quil MIDI Interaction"
  :size [500 500]
  :setup setup
  :update update-state
  :draw draw-state
  :features [:keep-on-top]
  :middleware [m/fun-mode bpm-middleware])
