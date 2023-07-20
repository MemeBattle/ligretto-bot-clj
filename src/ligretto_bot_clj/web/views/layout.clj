(ns ligretto-bot-clj.web.views.layout)

(defn Layout
  [title content]
  [:html
   [:head
    [:title title]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/daisyui@3.2.1/dist/full.css" :type "text/css"}]
    [:script {:src "https://cdn.tailwindcss.com?plugins=forms,typography"}]
    [:script {:src "https://unpkg.com/htmx.org@1.9.3"
              :integrity "sha384-lVb3Rd/Ca0AxaoZg5sACe8FJKF0tnUgR2Kd7ehUOG5GCcROv5uBIZsOqovBAcWua"
              :crossorigin "anonymous"}]]
   [:body
    [:div {:class "container"}
       content]]])
