![](https://github.com/kwladyka/consistency-clj/workflows/master%20tests/badge.svg)
![](https://github.com/kwladyka/consistency-clj/workflows/clojars/badge.svg)

# Consistency

Code which help me keep consistency in my projects. Feel free to use it.

## Rationale

- Consistency for all services which I develop.
- Don't reinvent the wheel in own services.
- Don't be force to use everything. Only what you want, how you want. It means good namespace splitting.

## Add dependency

[![Clojars Project](https://img.shields.io/clojars/v/net.clojars.kwladyka/consistency.svg)](https://clojars.org/net.clojars.kwladyka/consistency)

## Specification

- [logs/ring_wrappers](src/consistency/logs/ring_wrappers.clj) - wrappers for [ring](https://github.com/ring-clojure/ring)
- [logs/google/json_payload.clj](src/consistency/logs/google/json_payload.clj) - JSON structured logs output in google format. Especially useful for services like [cloud run](https://cloud.google.com/run), because google read JSON format from `stdout` and `stderr`. In that way you don't need any extra libraries for logging.
    - google doc [JSON payload](https://cloud.google.com/logging/docs/agent/logging/configuration#process-payload)
    - It works in [cloud run](https://cloud.google.com/run) with google [Error Reporting](https://cloud.google.com/error-reporting/docs/) without any additional settings.

---

Everything below this line is mainly for myself as a maintainer of this library.


### Tests

`clj -X:run-tests`

`clj -M:check-syntax-and-reflections`
