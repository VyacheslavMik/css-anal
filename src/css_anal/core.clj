(ns css-anal.core
  (:require [clojure.java.io :refer [file]]
            [clojure.string :as str]

            [css-anal.css-props :refer [css-props]]
            [css-anal.html-tags :refer [tags]])
  (:gen-class))

(defn clojure-file? [file]
  (let [s (str file)
        d (str/last-index-of s ".")]
    (when d
      (#{#_".clj" ".cljs" #_".cljc"} (subs s d)))))

(defn clojure-files [dir]
  (let [d (file dir)]
    (filter clojure-file? (file-seq d))))

(defn read-one
  [r]
  (try
    (read r)
    #_(read-string (str/replace r #"::\w+" "nil"))
    (catch java.lang.RuntimeException e
      (if (= "EOF while reading" (.getMessage e))
        ::EOF
        (throw e)))))

(defn read-seq-from-file
  "Reads a sequence of top-level objects in file at path."
  [path]
  (let [s (slurp path)
        s (str/replace s #"::\w+" "nil")]
    (with-open [#_(r (java.io.PushbackReader. (clojure.java.io/reader path)))
                r (java.io.PushbackReader. (java.io.StringReader. s))]
      (binding [*read-eval* false]
        (doall (take-while #(not= ::EOF %) (repeatedly #(read-one r))))))))

(defn extract-hiccups [sexp]
  (println sexp)
  (when sexp
    (if (and (vector? sexp) (keyword? (first sexp)))
      sexp
      (->> sexp
           (remove symbol?)
           (mapcat extract-hiccups)
           vec))))

(defn html-tag? [k]
  (when-let [tag (and (keyword? k)
                      (some->> (name k)
                               (re-matches #"^(\w+)\.?\#?.*")
                               second
                               keyword))]
    (and (tags tag))))

(defn extract-classes [hc]
  (cond
    (vector? hc) (mapcat extract-classes hc)
    (html-tag? hc) (re-seq #"\.\w+" (name hc))
    :else nil))

(defn css-class-name? [k]
  (and (keyword? k) (str/starts-with? (name k) ".")))

(defn css-properties? [m]
  (and m
       (every? (fn [[k _]]
                 (cond
                   (keyword? k) (css-props (name k))
                   (string? k) (css-props k)
                   :else false))
               m)))

(defn css-class? [sexp]
  (and (or (css-class-name? (first sexp))
           (html-tag? (first sexp)))
       (css-properties? (second sexp))))

;; (defn extract-garden-css [sexp]
;;   (

(defn get-all-classes [dir]
  (mapcat #(-> %
               read-seq-from-file
               extract-hiccups
               extract-classes
               distinct) (clojure-files dir)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(comment
  (get-all-classes "/home/evgeny/css-anal/src/css_anal")

  (def sample (read-seq-from-file (second (clojure-files "/home/evgeny/css-anal/src/css_anal_tmp/"))))

  (-> sample
      extract-hiccups
      extract-classes
      distinct)

  (def sample (read-seq-from-file (second (clojure-files "/Users/vyacheslavmikushev/Work/css-anal"))))

  (extract-hiccups sample)

  )
