(ns ligretto-bot-clj.web.views.home
  (:require
   [ligretto-bot-clj.web.views.layout :refer [Layout]]
   [ligretto-bot-clj.web.services.bot :as bot-service]))

(def ErrorIcon
  [:svg {:xmlns "http://www.w3.org/2000/svg", :class "stroke-current shrink-0 h-6 w-6", :fill "none", :viewbox "0 0 24 24"}
   [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"}]])

(defn WithError
  [ctx childrens]
  (let [error (get ctx :error)]
    (if (nil? error)
      childrens
      (conj
       childrens [:div.alert.alert-error.mt-4
                  ErrorIcon
                  [:div
                   [:span.font-bold "Error: "]
                   [:span error]]]))))

(defn BotCard
  [game-id {:keys [id strategy turn-timeout]}]
    [:div.flex.flex-col.bg-base-200.rounded-lg.shadow-lg.p-4
     [:p [:b "ID: "] id]
     [:p [:b "Strategy: "] (name strategy)]
     [:p [:b "Timeout: "] turn-timeout]
     [:button.btn.btn-outline.btn-error.mt-2
      {:hx-delete (str "/bots/" game-id "/" id)
       :hx-indicator "#indicator"
       :hx-target "#bots-list"
       :hx-confirm "Are you sure you want to delete this bot?"}
      "Delete"]])

(defn BotList
  [ctx]
  (let [{:keys [bots games]} (bot-service/get-all ctx)]
    [:div.flex.flex-row.flex-wrap
     {:id "bots-list"}
     (for [game games]
       (let [bots (get bots (:game-id game))
             game-id (some-> game :game-id name)]
         (when (not (nil? game-id))
           [:div.flex.flex-col
            [:h3.text-xl.font-bold.mb-4 (str "Game " game-id)]
            [:div.grid.grid-cols-3.gap-4
             (for [bot bots]
               (BotCard game-id bot))]])))]))

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
      {:hx-post "/create"
       :hx-target "#bots-list"
       :hx-indicator "#indicator"}
      [:input {:class       "input input-bordered input-bordered mb-3"
               :name        "room-url"
               :placeholder "Enter room URL"}]
      [:div.join
       [:input {:class       "input input-bordered input-bordered mb-3 join-item"
                :name        "turn-timeout"
                :placeholder "Turn timeout"
                :type        "number"
                :value       "1000"}]
       [:select.select.select-bordered.w-full.join-item
        {:name "strategy"
         :value "easy"}
        [:option {:value "easy"} "Easy"]
        [:option {:value "random"} "Random"]
        [:option {:value "default"} "Default"]]]
      [:button.btn.btn-primary {:type "submit"} "Add Bot"]]
     [:div.flex.flex-row.mb-4
      [:h2.text-3xl.font-bold.mr-2 "Active bots"]
      [:button.btn.btn-secondary.btn-sm
       {:hx-get "/list"
        :hx-target "#bots-list"
        :hx-indicator "#indicator"}
       "Refresh"]
      [:span.loading.loading-infinity.loading-lg.mr-2.text-secondary.htmx-indicator {:id "indicator"}]]
     (BotList ctx)]]
   Footer))
