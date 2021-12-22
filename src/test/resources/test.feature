@tag1
Feature: Test feature
  # To run standalone Karate features vie the IDE you temporarily need to comment out karate.abort() in karate-base.js

  @tag2
  Scenario: True
    * assert true

  Scenario: False
    * assert false
