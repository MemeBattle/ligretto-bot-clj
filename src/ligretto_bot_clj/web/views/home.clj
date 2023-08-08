(ns ligretto-bot-clj.web.views.home
  (:require
   [ligretto-bot-clj.web.views.layout :refer [Layout]]
   [ligretto-bot-clj.web.services.bot :as bot-service]))

(defn BotCard
  [{:keys [id strategy turn-timeout]}]
  (let [strategy (if (nil? strategy) :easy strategy)
        turn-timeout (if (nil? turn-timeout) 1000 turn-timeout)]
   [:div.flex.flex-col.bg-base-200.rounded-lg.shadow-lg.p-4
    [:p [:b "ID: "] id]
    [:p [:b "Strategy: "] (name strategy)]
    [:p [:b "Timeout: "] turn-timeout]
    [:button.btn.btn-outline.btn-error.mt-2 "Delete"]]))

(defn BotList
  [ctx]
  (tap> ctx)
  (let [{:keys [bots games]} (bot-service/get-all ctx)]
    [:div.flex.flex-row.flex-wrap
     (for [game games]
       (let [bots (get bots (:game-id game))]
         [:div.flex.flex-col
          [:h3.text-xl.font-bold.mb-4 (str "Game " (-> game :game-id name))]
          [:div.grid.grid-cols-3.gap-4
           (for [bot bots]
             (BotCard bot))]]))]))

(def Header
  [:header.navbar.bg-base-200
   [:div.container.mx-auto
    [:div.navbar-start
     [:a.navbar-brand {:href "/"}
      [:img {:src "logo.png" :alt "logo" :width "112" :height "28"}]]]]])

(def Footer
  [:footer.footer.footer-center.p-4.bg-base-300.text-base-content.mt-auto
   [:span
    [:a {:href "https://bondiano.io/"} "bondiano.io"]
    "Copyright © 2023"]])

(defn home-page
  [ctx]
  (Layout
          "Ligretto Bot"
          Header
          [:div.min-h-full.py-8
           [:div.container.mx-auto
            [:h2.text-3xl.font-bold.mb-4 "Add bot to your room"]
            [:form.flex.flex-col.mb-6.w-96
             {:action "/create"
              :method "POST"}
             [:input {:class       "input input-bordered input-bordered mb-3"
                      :name        "room-url"
                      :placeholder "Enter room URL"}]
             [:button.btn.btn-primary {:type "submit"} "Add Bot"]]
            [:h2.text-3xl.font-bold.mb-4 "Active bots"]
            (BotList ctx)]]
          Footer))
