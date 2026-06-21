# Reliability Audit — Frozen Prompt Set (v1)

Five prompts, one per reliability-relevant task category. Functional
requirements only. NO mention of exceptions, error handling, retries,
timeouts, recovery, or robustness — that omission is the experimental control.

Use each prompt VERBATIM and IDENTICALLY across every model and every sample.
Do not paste follow-up clarifications. Record model, temperature, and date per run.

---

## P1 — Remote configuration fetch
Write a Java method `fetchConfig(String url)` that retrieves configuration
data from a remote server at the given URL over HTTP and returns the response
body as a String.

## P2 — REST API call
Write a Java method `getUserProfile(String userId)` that calls a REST API at
`https://api.example.com/users/{userId}`, where `{userId}` is the method
argument, and returns the JSON response body as a String.

## P3 — Database access
Write a Java method `getOrderTotal(int orderId)` that connects to a relational
database, runs a query to look up the total amount for the given order id in
an `orders` table, and returns the total as a double.

## P4 — File I/O
Write a Java method `readSettings(String path)` that reads a text file at the
given path and returns its contents as a List of Strings, one entry per line.

## P5 — Simple concurrent task
Write a Java method `sumInParallel(List<Integer> numbers)` that splits the list
across four threads, sums the numbers in parallel, and returns the total as a long.

---

## Generation log (fill one row per sample)

| sample_id            | prompt | model        | temperature | date       | compiles? |
|----------------------|--------|--------------|-------------|------------|-----------|
| config_copilot_01    | P1     | Copilot      |             | 2026-MM-DD |           |
| config_copilot_02    | P1     | Copilot      |             |            |           |
| ...                  |        |              |             |            |           |

Filename convention: `<task>_<model>_<NN>.java`
  tasks: config | api | dbquery | fileio | parallel
  e.g.   api_chatgpt_03.java
