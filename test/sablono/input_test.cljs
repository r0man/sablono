(ns sablono.input-test
  ;; (:require [devcards.core :refer-macros [defcard]]
  ;;           [clojure.pprint :refer [pprint]]
  ;;           [rum.core :as rum])
  )

;; (def fruits
;;   [["grapefruit" "Grapefruit"]
;;    ["lime" "Lime"]
;;    ["coconut" "Coconut"]
;;    ["mango" "Mango"]])

;; (defn input-value [event]
;;   (let [target (.-target event)]
;;     (case (.-type target)
;;       "checkbox" (.-checked target)
;;       (.-value target))))

;; (defmulti on-change (fn [type state] type))

;; (defmethod on-change :checkbox [type state]
;;   #(swap! state not))

;; (defmethod on-change :radio [type state]
;;   #(swap! state not))

;; (defmethod on-change :default [type state]
;;   #(reset! state (input-value %1)))

;; (rum/defc show-value < rum/reactive [state]
;;   [:pre (with-out-str (pprint (rum/react state)))])

;; (rum/defc controlled-input < rum/reactive [type state]
;;   [:div
;;    [:input
;;     {:on-change (on-change type state)
;;      :type (name type)
;;      (case type
;;        :checkbox :checked
;;        :radio :checked
;;        :value) (rum/react state)}]
;;    (show-value state)])

;; (rum/defc uncontrolled-input [type state]
;;   [:div
;;    [:input
;;     {:on-change (on-change type state)
;;      :type (name type)}]
;;    (show-value state)])

;; (rum/defc controlled-select < rum/reactive [state]
;;   (let [{:keys [options selected]} (rum/react state)]
;;     [:div
;;      [:select
;;       {:on-change #(swap! state assoc :selected (input-value %1))
;;        :value selected}
;;       (for [[value description] options]
;;         [:option {:key value :value value} description])]
;;      (show-value state)]))

;; (rum/defc uncontrolled-select < rum/reactive [state]
;;   (let [{:keys [options selected]} (rum/react state)]
;;     [:div
;;      [:select
;;       {:default-value selected
;;        :on-change #(swap! state assoc :selected (input-value %1))}
;;       (for [[value description] options]
;;         [:option {:key value :value value}
;;          description])]
;;      (show-value state)]))

;; (defcard checkbox-uncontrolled
;;   (uncontrolled-input :checkbox (atom nil)))

;; (defcard checkbox-controlled
;;   (controlled-input :checkbox (atom "")))


;; (defcard text-uncontrolled
;;   (uncontrolled-input :text (atom nil)))

;; (defcard text-controlled
;;   (controlled-input :text (atom "")))


;; (defcard textarea-uncontrolled
;;   (uncontrolled-input :textarea (atom nil)))

;; (defcard textarea-controlled
;;   (controlled-input :textarea (atom "Hello")))


;; (defcard select-uncontrolled
;;   (uncontrolled-select (atom {:options fruits :selected "coconut"})))

;; (defcard select-controlled
;;   (controlled-select (atom {:options fruits :selected "coconut"})))


;; (rum/defc select-toggle < rum/reactive [opts]
;;   [:div
;;    [:button
;;     {:on-click #(swap! opts (fn [opts] (if (seq opts) [] ["1st" "2nd" "3rd"])))}
;;     "Toggle"]
;;    [:select
;;     (map
;;      (fn [opt] [:option {:key opt :value opt} opt])
;;      (rum/react opts))]])

;; (defcard select-toggle-issue-145
;;   (select-toggle (atom ["1st" "2nd" "3rd"])))
