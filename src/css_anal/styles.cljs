(ns css-anal.styles
  (:require [garden.selectors :as s]))

(println #js {"a" 1})

(defn switch []
  [:.switch
   [:label {:cursor :pointer}
    [:&.invalid :&:focus.invalid {:bottom "0px"}]
    [:a.button {:display :block}]
    [:div {:display :flex}
     [:span {:font-size "14px"
             :letter-spacing "0.4px"}]]]
   [:.lever {:bottom "0px"
             :content "''"
             :cursor "pointer"
             :display "inline-block"
             :height "15px"
             :left "0px"
             :position "relative"
             :right "0px"
             :top "0px"
             :vertical-align "middle"
             :width "30px"
             :perspective-origin "20px 7.5px"
             :transform-origin "20px 7.5px"
             :background-color "#c5c5c5"
             :border-radius "15px"
             :margin "0px 16px"
             :transition "background 0.3s ease"}
    [:&:after {:bottom "-3px"
               :box-shadow "rgba(0, 0, 0, 0.12) 0px 1px 1px 1px"
               :content "''"
               :cursor "pointer"
               :display "block"
               :height "21px"
               :left "-5px"
               :position "absolute"
               :right "-5px"
               :top "-3px"
               :width "21px"
               :perspective-origin "10.5px 10.5px"
               :transform-origin "10.5px 10.5px"
               :background-color "#F1F1F1"
               :border-radius "21px"
               :transition "left 0.3s ease, background 0.3s ease, box-shadow 0.1s ease"}]]
   [(s/input (s/attr= "type" "checkbox")) {:opacity 0
                                           :width 0
                                           :height 0}]
   [(s/+ (s/input (s/attr= "type" "checkbox") (s/checked)) :.lever) {:background-color "rgba(22, 171, 127, 0.5)"}
    [:&:after {:left "14px"
               :background-color "#16ab7f"}]]])
