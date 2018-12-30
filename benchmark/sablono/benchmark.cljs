(ns sablono.benchmark
  (:require [perforate-x.core :as perf :refer [defgoal defcase]]
            [react :as React]
            [reagent.impl.template :as reagent]
            [sablono.core :as html :refer-macros [html]]
            [sablono.server :refer [render-static]]))

(defn render [x]
  (render-static x))


(defgoal :compile-tag-only
  "Render element with tag only")

(defcase :compile-tag-only :sablono []
  #(render (html [:div])))

(defcase :compile-tag-only :react []
  #(render (React/createElement "div")))

(defcase :compile-tag-only :reagent []
  #(render (reagent/as-element [:div])))



(defgoal :compile-class-attribute
  "Render element with class attribute")

(defcase :compile-class-attribute :sablono []
  #(render (html [:div.x])))

(defcase :compile-class-attribute :react []
  #(render (React/createElement "div" #js {:className "x"})))

(defcase :compile-class-attribute :reagent []
  #(render (reagent/as-element [:div.x])))



(defgoal :compile-class-and-id-attributes
  "Render element with class and id attribute")

(defcase :compile-class-and-id-attributes :sablono []
  #(render (html [:div#x.y])))

(defcase :compile-class-and-id-attributes :react []
  #(render (React/createElement "div" #js {:className "y" :id "x"})))

(defcase :compile-class-and-id-attributes :reagent []
  #(render (reagent/as-element [:div#x.y])))



(defgoal :compile-nested-literals
  "Render nested elements")

(defcase :compile-nested-literals :sablono []
  #(render (html [:div
                  [:h3 "I am a component!"]
                  [:p.someclass
                   "I have " [:strong "bold"]
                   [:span {:style {:color "red"}} " and red"]
                   " text."]])))

(defcase :compile-nested-literals :react []
  #(render (React/createElement
            "div" nil
            (React/createElement "h3" nil "I am a component!")
            (React/createElement
             "p" #js {:className "someclass"}
             "I have " (React/createElement "strong" nil "bold")
             (React/createElement "span" #js {:style #js {:color "red"}} " and red")
             " text."))))

(defcase :compile-nested-literals :reagent []
  #(render (reagent/as-element
            [:div
             [:h3 "I am a component!"]
             [:p.someclass
              "I have " [:strong "bold"]
              [:span {:style {:color "red"}} " and red"]
              " text."]])))


(defgoal :interpret-attributes
  "Render elements with interpreted attributes")

(defcase :interpret-attributes :sablono []
  #(render (html [:div ((constantly {:class "x"}))])))

(defcase :interpret-attributes :react []
  #(render (React/createElement "div" ((constantly #js {:className "x"})))))

(defcase :interpret-attributes :reagent []
  #(render (reagent/as-element [:div ((constantly {:class "x"}))])))



(defgoal :interpret-hinted-attributes
  "Render elements with interpreted attributes")

(defcase :interpret-hinted-attributes :sablono []
  #(render (html [:div ^:attrs ((constantly {:class "x"}))])))

(defcase :interpret-hinted-attributes :react []
  #(render (React/createElement "div" ((constantly #js {:className "x"})))))

(defcase :interpret-hinted-attributes :reagent []
  #(render (reagent/as-element [:div ((constantly {:class "x"}))])))


(defgoal :compile-attributes-children
  "Render element with literal attributes and children")

(defcase :compile-attributes-children :sablono []
  #(render (html [:div {:class "a"} "b" 1 2 3])))

(defcase :compile-attributes-children :react []
  #(render (React/createElement "div" #js {:className "a"} "b" 1 2 3)))

(defcase :compile-attributes-children :reagent []
  #(render (reagent/as-element [:div {:class "a"} "b" 1 2 3])))



(defgoal :compile-when-form
  "Render element with literal attributes and children")

(defcase :compile-when-form :sablono []
  #(render (html [:div {:class "a"}
                  (when true [:div [:div]])])))

(defcase :compile-when-form :react []
  #(render (React/createElement
            "div" #js {:className "a"}
            (when true
              (React/createElement
               "div" nil (React/createElement "div"))))))

(defcase :compile-when-form :reagent []
  #(render (reagent/as-element
            [:div {:class "a"}
             (when true [:div [:div]])])))

(perf/run-goals)
