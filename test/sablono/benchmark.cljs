(ns sablono.benchmark
  (:require-macros [cemerick.cljs.test :refer [is deftest testing]]
                   [dommy.macros :refer [node]]
                   [sablono.core :refer [html]])
  (:require [cloact.core :as cloact]
            [crate.core :as crate]
            [dommy.template :as template]
            [goog.dom :as gdom]
            [sablono.test :refer [body]]))

(defn cloact-template [datum]
  [:li [:a {:href (str "#show/" (:key datum))}]
   [:div {:id (str "item" (:key datum))
          :className ["class1" "class2"]}
    [:span {:className "anchor"} (:name datum)]]])

(defn crate-template [datum]
  (crate/html
   [:li [:a {:href (str "#show/" (:key datum))}]
    [:div {:id (str "item" (:key datum))
           :class ["class1" "class2"]}
     [:span {:class "anchor"} (:name datum)]]]))

(defn dommy-template [datum]
  (template/node
   [:li [:a {:href (str "#show/" (:key datum))}
         [:div.class1.class2 {:id (str "item" (:key datum))}
          [:span.anchor (:name datum)]]]]))

(defn dommy-compiled [datum]
  (node
   [:li [:a {:href (str "#show/" (:key datum))}
         [:div.class1.class2 {:id (str "item" (:key datum))}
          [:span.anchor (:name datum)]]]]))

(defn jquery-template [datum]
  (-> "<li>" js/jQuery
      (.append
       (-> "<a>" js/jQuery
           (.attr "href" (str "#show/" (:key datum)))
           (.addClass "anchor")
           (.append (-> "<div>" js/jQuery
                        (.addClass "class1")
                        (.addClass "class2")
                        (.attr "id" (str "item" (:key datum)))
                        (.append (-> "<span>" js/jQuery (.text (:name datum))))))))))

(defn react-template [datum]
  (js/React.DOM.li
   (js/React.DOM.a #js {:href (str "#show/" (:key datum))})
   (js/React.DOM.div
    #js {:id (str "item" (:key datum))
         :className "class1 class2"}
    (js/React.DOM.span #js {:className "anchor"} (:name datum)))))

(defn sablono-template [datum]
  (html [:li [:a {:href (str "#show/" (:key datum))}]
         [:div {:id (str "item" (:key datum))
                :class ["class1" "class2"]}
          [:span {:class "anchor"} (:name datum)]]]))

(defn run-test [root data li-fn render-fn]
  (let [now (js/Date.)]
    (render-fn root (map li-fn data))
    (/ (- (.getTime (js/Date.))
          (.getTime now)) 1000)))

(defn gen-data []
  (for [i (range 1e4)]
    {:key (rand-int 1e6)
     :name (str "product" i)}))

(defn render-append [root children]
  (let [ul (goog.dom/createDom "ul")]
    (doseq [child children]
      (goog.dom/append ul child))
    (goog.dom/append root ul)))

(defn render-cloact [root children]
  (cloact/render-component [:ul children] root))

(defn render-react [root children]
  (let [render-fn #(this-as this (html [:ul children]))
        component (js/React.createClass #js {:render render-fn})]
    (js/React.renderComponent (component) root)))

(defn time-test [data]
  (for [[key li-fn render-fn]
        (shuffle
         [[:cloact cloact-template render-cloact]
          [:crate crate-template render-append]
          [:dommy dommy-template render-append]
          [:dommy-compiled dommy-compiled render-append]
          [:jquery jquery-template render-append]
          [:react react-template render-react]
          [:sablono sablono-template render-react]])]
    (let [root (goog.dom/createDom "div")
          _ (goog.dom/append (body) root)
          secs (run-test root data li-fn render-fn)]
      [key secs])))

(deftest perf-test []
  (let [data (doall (gen-data))]
    (prn (->> (for [i (range 3)]
                (into {} (time-test data)))
              (reduce (partial merge-with +))
              (map (fn [[k v]] [k (/ v 3)]))
              (into {})))))
