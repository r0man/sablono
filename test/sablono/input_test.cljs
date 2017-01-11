(ns sablono.input-test
  (:require [devcards.core :refer-macros [defcard]]
            [rum.core :as rum]))

(defn input-value [event]
  (let [target (.-target event)]
    (case (.-type target)
      "checkbox" (.-checked target)
      (.-value target))))

(defmulti on-change (fn [type state] type))

(defmethod on-change :checkbox [type state]
  #(swap! state not))

(defmethod on-change :radio [type state]
  #(swap! state not))

(defmethod on-change :default [type state]
  #(reset! state (input-value %1)))

(rum/defc show-value < rum/reactive [state]
  [:span
   {:style {:margin-left 15}}
   [:b "Value: "]
   (pr-str (rum/react state))])

(rum/defc controlled-input < rum/reactive [type state]
  [:div
   [:input
    {:on-change (on-change type state)
     :type (name type)
     (case type
       :checkbox :checked
       :radio :checked
       :value) (rum/react state)}]
   (show-value state)])

(rum/defc uncontrolled-input [type state]
  [:div
   [:input
    {:on-change (on-change type state)
     :type (name type)}]
   (show-value state)])

(defcard checkbox-controlled
  (controlled-input :checkbox (atom nil)))

(defcard checkbox-controlled-initial
  (controlled-input :checkbox (atom true)))

(defcard checkbox-uncontrolled
  (uncontrolled-input :checkbox (atom nil)))

(defcard text-controlled
  (controlled-input :text (atom nil)))

(defcard text-controlled-initial
  (controlled-input :text (atom "Hello")))

(defcard text-uncontrolled
  (uncontrolled-input :text (atom nil)))

(defcard textarea-controlled-initial
  (controlled-input :textarea (atom "Hello")))

(defcard textarea-controlled
  (controlled-input :textarea (atom nil)))

(defcard textarea-uncontrolled
  (uncontrolled-input :textarea (atom nil)))
