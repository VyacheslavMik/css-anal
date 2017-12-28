(ns css-anal.hiccup)

(defn my-form []
  [:form.auth
   [:div.auth-wrapper {:style {:border "1px solid red"}}
    [:div.auth-box {:class (when true :active)}
     [:div.header
      [:div.logo
       [:img {:src "images/logo_login.svg" :height "30px"}]]]
     [:div.fields
      [email-field]
      [:div.field.password
       [password-input]
       [:div.sublink
        [:a {:href "google.com"} "Forgot Password?"]]]]
     [:div.buttons
      [:div.link
       [:a {:href "google.com"} "Sign Up"]]]]]])
