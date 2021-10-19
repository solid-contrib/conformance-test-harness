Feature: Test feature
  # To run standalone Karate features vie the IDE you temporarily need to comment out karate.abort() in karate-base.js

  Background:
#    * def hasStorageType = function(s) { return s.includes('http://www.w3.org/ns/pim/space#Storage') && s.includes('rel="type"') }
#    * def hasStorageLink = function(s) { const res = s.findIndex(elem => { karate.log(elem); return elem.includes('rel="http://www.w3.org/ns/pim/space#storage"')}); karate.log('res = ', res); return res }
#    * def hasStorageType = function(s) { return s.includes('http://www.w3.org/ns/pim/space#Storage') && s.includes('rel="type"') }
#    * def hasStorageLink = function(s) { const res = s.includes('rel="http://www.w3.org/ns/pim/space#storage"'); karate.log('res = ', res); return res }

    * def hasStorageType = (s) => s.includes('http://www.w3.org/ns/pim/space#Storage') && s.includes('rel="type"')
    * def hasStorageLink = (s) => s.includes('rel="http://www.w3.org/ns/pim/space#storage"')


  Scenario: True
    * assert true

  Scenario: False
    * assert false

  Scenario: Test matching
    * def data = {"Date":["Fri, 15 Oct 2021 11:21:52 GMT"],"Content-Type":["text/plain"],"Connection":["keep-alive"],"Accept-Ranges":["bytes"],"Cache-Control":["must-revalidate, no-transform, max-age=0, private"],"ETag":["\"3dc8b16348ffdf75916dc742326a724fe9dad5e76e293529164c92c2d1fe0c4f\""],"Vary":["Accept,Origin,Accept-Datetime,Range"],"Accept-Patch":["application/sparql-update"],"Last-Modified":["Fri, 15 Oct 2021 11:21:51 GMT"],"link":["<https://pod.inrupt.com/solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt>; rel=\"canonical\"","<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"","</solid-test-suite-alice/>; rel=\"http://www.w3.org/ns/pim/space#storage\"","<https://pod.inrupt.com/solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt>; rel=\"self\"","<http://www.w3.org/ns/ldp#NonRDFSource>; rel=\"type\"","<https://pod.inrupt.com/solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt?ext=description>; rel=\"describedby\"","</powerswitch/solid-test-suite-alice>; rel=\"https://inrupt.com/ns/ess#hasPowerSwitch\"","<https://pod.inrupt.com/solid-test-suite-alice/profile/card#me>; rel=\"http://www.w3.org/ns/solid/terms#podOwner\"","</solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt?ext=shex>; rel=\"http://www.w3.org/ns/shex#Schema\"","<https://pod.inrupt.com/solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt>; rel=\"original timegate\"","</solid-test-suite-alice/shared-test/89e7717f-53fc-4926-8b65-4d75f3644d97/609bfdd9-ac23-41f6-8b0d-36f96d38acf4/8ad13301-1277-4b6d-81d4-a67ad8eb9abe.txt?ext=acr>; rel=\"http://www.w3.org/ns/solid/acp#accessControl\"","<http://www.w3.org/ns/solid/acp#Read>; rel=\"http://www.w3.org/ns/solid/acp#allow\"","<http://www.w3.org/ns/solid/acp#Write>; rel=\"http://www.w3.org/ns/solid/acp#allow\""],"Allow":["GET,HEAD,OPTIONS,PUT,DELETE"],"Strict-Transport-Security":["max-age=15724800; includeSubDomains"]}
    * def date = { Link: [3, 15], a: 20 }
    * def isValidMonth = function(m) { const res = m.findIndex(elem => elem >= 0 && elem <= 12); karate.log(res); return res }
    * match data.link contains '#? hasStorageLink(_)'
    * match data.link !contains '#? hasStorageType(_)'
    * match data.link !contains '#? hasStorageLink(_)'

  Scenario Outline: Exception handling
    Given def number = java.lang.Integer.parseInt("<number>")
    Then print number

    Examples:
      | number     |
      | 10         |
      | notANumber |

  Scenario: Example exception without message from java
# Actual stack trace is something like the following:
#  java.util.ConcurrentModificationException
#     at examples.NoMessageException.error(NoMessageException.java:8)
#     at examples.ExamplesRunner.exceptionFromJava(ExamplesRunner.java:22)

# Karate output with https://github.com/intuit/karate/commit/608895d6d958deebed7ab942b2cf6d2d170b9d1c :
#  example.feature:31 - evaluation (js) failed: e.error(), e.error()
#  stack trace: examples.NoMessageException.error(NoMessageException.java:8)

# Karate output with e.toString()
#  example.feature:35 - evaluation (js) failed: e.error(), java.util.ConcurrentModificationException
#  stack trace: examples.NoMessageException.error(NoMessageException.java:8)
    Given def e = Java.type("examples.NoMessageException")
    Then e.error()
