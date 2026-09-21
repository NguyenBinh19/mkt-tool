# Facebook Comment Extractor

Spring Boot API that opens a persistent local Chromium session, lets the operator sign in to Facebook manually, then extracts the comments currently available to that signed-in account from a supplied Facebook post URL.

## Run

Use Java 21 and Maven. Install the Playwright Chromium browser once if it is not already installed:

```bash
mvn exec:java "-Dexec.mainClass=com.microsoft.playwright.CLI" "-Dexec.args=install chromium"
mvn spring-boot:run
```

The first step is only needed when Playwright reports that its Chromium executable is missing.

## Sign in once

Open `GET http://localhost:8080/api/facebook/browser`. A visible Chromium window opens using `../playwright-profile` by default. Sign in yourself and complete any Facebook checkpoint or 2FA there. The profile is local-only and is excluded from version control; credentials are never put in `application.yaml`.

## Extract

Send a request like this:

```bash
curl -X POST http://localhost:8080/api/facebook/extract \
  -H 'Content-Type: application/json' \
  -d '{"postUrl":"https://www.facebook.com/PAGE/posts/POST_ID","maxComments":200,"includeReplies":true}'
```

`maxComments` defaults to 200 and is capped by `facebook.max-comments-limit` (1,000 by default). The extractor expands visible “more comments” controls, progressively scrolls, de-duplicates comment IDs, and stores newly found comments in the configured MySQL database.

Facebook can alter its markup, restrict comments by privacy settings, or show additional verification. In those cases the API returns a clear authentication/error status rather than attempting to bypass the restriction.
