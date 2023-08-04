(ns ligretto-bot-clj.web.views.home
  (:require
   [hiccup.page :refer [html5]]

   [ligretto-bot-clj.web.views.layout :refer [Layout]]))

(defn Bot
  []
  [:div.flex.flex-col
   [:p "ID: Name"]
   [:p "Status: Running"]
   [:p.mb-2 "Room: ID"]
    [:button.btn.btn-outline.btn-error "Delete"]])

(defn BotList
  [ctx]
  [:div.grid.grid-cols-3.gap-4
   (for [i (range 10)]
     [:div {:class "p-4 bg-base-200"} (Bot)])])

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
