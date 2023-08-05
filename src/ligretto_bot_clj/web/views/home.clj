(ns ligretto-bot-clj.web.views.home
  (:require
   [hiccup.page :refer [html5]]

   [ligretto-bot-clj.web.views.layout :refer [Layout]]
   [ligretto-bot-clj.web.services.bot :as bot-service]))

(defn BotCard
  [bot]
  [:div.flex.flex-col.bg-base-200.rounded-lg.shadow-lg.p-4
   [:p [:b "ID: "] (:id bot)]
   [:p [:b "Strategy: "] (name (:strategy bot))]
   [:p [:b "Timeout: "] (:turn-timeout bot)]
   [:button.btn.btn-outline.btn-error.mt-2 "Delete"]])

(defn BotList
  [ctx]
  (let [{:keys [bots games]} (bot-service/get-all ctx)]
    [:div.felx.flex-row.flex-wrap
     (for [game games]
       [:div.flex.flex-col
        [:h3.text-xl.font-bold.mb-4 (str "Game " (-> game :game-id name))]
        [:div.grid.grid-cols-3.gap-4
         (for [bot (get bots (:game-id game))]
           (BotCard bot))]])]))

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
  (html5 (Layout
          "Ligretto Bot"
          Header
          [:div.min-h-full.py-8
           [:div.container.mx-auto
            [:h2.text-3xl.font-bold.mb-4 "Add bot to your room"]
            [:form.flex.flex-col.mb-6.w-96
             [:input {:class       "input input-bordered input-bordered mb-3"
                      :id          "room-url"
                      :type        "text"
                      :placeholder "Enter room URL"}]
             [:button.btn.btn-primary "Add Bot"]]
            [:h2.text-3xl.font-bold.mb-4 "Active bots"]
            (BotList ctx)]]
          Footer)))
