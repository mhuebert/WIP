(ns wip.graphql.request
  (:refer-clojure :exclude [catch])
  (:require ["unfetch" :as unfetch]
            [wip.graphql.printer :as string]
            [applied-science.js-interop :as j]
            [chia.util :as u]
            [kitchen-async.promise :as p]))

(defn ->promise ^js [x]
  (cond-> x
          (not (u/promise? x)) (js/Promise.resolved)))

(defn then [x f]
  (.then (->promise x) f))

(defn catch [^js x f]
  (.catch x f))

(defn get-token [token]
  (cond (u/promise? token) (then token get-token)
        (fn? token) (get-token (token))
        :else (js/Promise.resolve token)))

(defn fetch [url options]
  (unfetch url (-> options
                   (update :body (comp js/JSON.stringify clj->js))
                   (clj->js))))

(defn post!
  [api-opts-promise form variables]
  (let [query-string (-> (string/emit form)
                         :string+)]
    (assert (string? query-string))
    (p/let [{:as           options
             :request/keys [get-token
                            url]} api-opts-promise
            token (get-token)
            response (fetch url
                            {:method      "POST"
                             :credentials "include"
                             :headers     (cond-> {:Content-Type "application/json"}
                                                  token (assoc :Authorization (str "Bearer: " token)))
                             :body        {:query     query-string
                                           :variables variables}})]
      (j/call response :json))))

