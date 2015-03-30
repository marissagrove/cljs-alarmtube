(ns cljs-alarmtube.core
  (:require [clojure.string :as s]
            [goog.date.DateTime]
            [goog.i18n.DateTimeFormat]
            [goog.i18n.DateTimeFormat.Format]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(def formatter
  (goog.i18n.DateTimeFormat. goog.i18n.DateTimeFormat.Format/SHORT_TIME))

(def app-state (atom {:debug? true
                      :text "Alarmtube"
                      :current-time (goog.date.DateTime.)
                      :set-time {:hour 3 :minute 57}
                      :video [{:id "4NZdggNUvq0"} {:id "oe1wtkkt9-E"} {:id "KQ5yjAH7bxM"} {:id "rfUYuIVbFg0"} {:id "c90w77hmaa8"}]
                      :alarm-set? false
                      :player-info {:playing? false}}))

(defn debug-header [payload owner opts]
  (reify
    om/IRender
    (render [_]
      (html [:div (pr-str payload)]))))

(defn time-form [payload owner opts]
  (reify
    om/IRender
    (render [_]
      (html [:form {:action ""
                    :method ""}
        [:div#alarmclock
         [:div
          [:div.leftcolumn "Current Time:" (.format formatter (:current-time payload))]]
         [:div
          [:div.leftcolumn "Set Alarm:"]
          [:span
           [:select {:on-change (fn [event]
                                  (.stopPropagation event)
                                  (om/transact! payload [:set-time :hour]
                                                (fn [current-value]
                                                  (let [new-value (js/parseInt (.-value (.-target event)))]
                                                    new-value)))) 
                     :value (get-in payload [:set-time :hour])}
            (map (fn [number]
                   [:option {:value number} number])(range 1 13))]]
          [:span
           [:select {:on-change (fn [event]
                                  (.stopPropagation event)
                                  (om/update! payload [:set-time :minute] (js/parseInt (.-value (.-target event)))))
                     :value (get-in payload [:set-time :minute])}
            (map (fn [number]
                   [:option {:value number} number])(range 0 60 5))]]
          [:span
           [:select {:on-change (fn [event]
                                  (.stopPropagation event)
                                  (om/update! payload [:set-time :am?] (= "am" (.-value (.-target event)))))
                     :value (if (get-in payload [:set-time :am?])
                              "am"
                              "pm")}
            [:option "am"]
            [:option "pm"]]]]
         [:div
          [:div.leftcolumn "Set Alarm Action:"]
          [:input#submitbutton {:on-click (fn [event]
                                            (.stopPropagation event)
                                            (.preventDefault event)
                                            (om/update! payload [:alarm-set?] true))
                                :type "submit"
                                :value "set alarm!"}]]]]))))

(defn youtube-player [payload owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (js/console.log "Youtube player will mount"))
    om/IDidMount
    (did-mount [_]
      (let [on-video-ready (fn [event]
                             ;; on-video-ready is called video the YT installer when the video is ready
                             ;; Event is the onReady event from the YT installer
                             ;; It's fired once the iframe is loaded, the videos are loaded, and we can actually play a video
                             ;; We want to put the player itself into app state here
                             ;; The installer passes the player to on-video-ready as an attribute of event, in event.target
                             ;; Put the player into the app-state at :player
                             (om/update! payload :player (.-target event)))
            installer (js/YT.Player. "ytapiplayer" #js{:height 356
                                                       :width 425
                                                       :videoId "4NZdggNUvq0"
                                                       :events #js {:onReady on-video-ready}})]
        (om/update! payload [:installer] installer))
      (js/console.log "Youtube player did mount"))
    om/IRender
    (render [_]
      (html
       [:div
        [:div#ytapiplayer "The video will load here, GIVE IT A SECOND PLEASE. Or an error has occured, in which case, don't wait around."]
        [:button {:on-click (fn [event]
                              (let [player (:player @payload)]
                                (if (get-in payload [:player-info :playing?])
                                  (do (.stopVideo player)
                                      (om/update! payload [:player-info :playing?] false))
                                  (do (.playVideo player)
                                      (om/update! payload [:player-info :playing?] true)))))}
         (if (get-in payload [:player-info :playing?])
           "Pause"
           "Play")]]))))

(defn render-root [element-id]
  (om/root
   (fn [app owner]
     (reify om/IRender
       (render [_]
         (html
          [:div
           [:button {:on-click #(om/transact! app [:debug?] not)} "Toggle Debug"]
           (when (:debug? app)
             (om/build debug-header app))
           [:label#playlist-label {:for "playlist"} "Video ID's (separated by commas)"]
           [:input#playlist {:on-change (fn [event]
                                          (om/update! app [:video]
                                                      (mapv (fn [id] {:id id})
                                                            (s/split (.-value (.-target event)) #","))))
                             :type "text"
                             :value (s/join "," (map :id (get app :video)))}]
           [:h1 (get app :text)]
           (om/build time-form app)
           (om/build youtube-player app)
           [:br]
           [:input#stop-video {:type "submit"
                               :value "I'm Awake!"}]]))))
   app-state
   {:target (. js/document (getElementById element-id))}))

(defn start [element-id]
  (js/setInterval (fn []
                    (let [new-time (goog.date.DateTime.)
                          am? (> (.getHours new-time) 12)
                          al-set? (get-in @app-state [:alarm-set?])
                          al-hour (get-in @app-state [:set-time :hour])
                          al-minute (get-in @app-state [:set-time :minute])
                          al-am? (get-in @app-state [:set-time :am?])
                          already-playing? (get-in @app-state [:player-info :playing?])
                          alarm-triggered? (if (<= al-hour (.getHours new-time))
                                               (<= al-minute (.getMinutes new-time)))]
                      (if al-set?
                        (when alarm-triggered?
                          (do (.setVolume (.playVideo (:player @app-state)) 100)
                              (om/update! app-state already-playing? true))) true)
                      (swap! app-state (fn [app]
                                         (assoc app :current-time new-time)))))
                  1000)
  (render-root element-id))

(defn get-yt-player []
  (:player @app-state))
