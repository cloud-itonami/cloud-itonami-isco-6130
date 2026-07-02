(ns mixed-farming.governor
  "FarmOpsGovernor — the independent safety/traceability layer for the
  ISCO-08 6130 independent mixed-crop-and-animal-producer actor. Wired
  as its own `:govern` node in `mixed-farming.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of plot
  provenance or chemical/livestock risk, so this MUST be a separate
  system able to reject a proposal (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. plot provenance    — the request's plot must be registered.
    2. no-actuation       — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    3. chemical treatment on a plot with `near-water?` true.
    4. livestock feeding/medication on a plot with `has-livestock?` true
       whose op is anything other than plain :feed-livestock (i.e. any
       treatment/medication op).
    5. low confidence (< `confidence-floor`)."
  (:require [mixed-farming.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} plot-record]
  (cond-> []
    (nil? plot-record)
    (conj {:rule :no-plot :detail (str "未登録 plot " (:plot-id request))})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn- near-water-chemical? [plot-record proposal]
  (and (:near-water? plot-record) (= :treat-crop (:op proposal))))

(defn- livestock-medication? [plot-record proposal]
  (and (:has-livestock? plot-record)
       (not (contains? #{:feed-livestock :harvest} (:op proposal)))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `mixed-farming.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [plot-record (store/plot store (:plot-id request))
        hard (hard-violations {:request request :proposal proposal} plot-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky? (boolean
                (and plot-record
                     (or (near-water-chemical? plot-record proposal)
                         (livestock-medication? plot-record proposal))))]
    {:ok? (and (not hard?) (not low?) (not risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky?))}))
