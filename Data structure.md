#Data structures

The following sections outline the data contributing to the coverage and conformance reports. 
The coverage report only requires the assertor description and the test suite description document whereas the conformance
report uses data from all these sources.

##Config
An input provided by the tester. Note this should be split so that the description of the test harness comes from the
harness software and the tester only provides a description of the servers under test.
```turtle
  <harness> a earl:Software .
  <ess> a earl:Software, earl:TestSubject .
```

##Suite Description
For each specification document there will be a test suite description document describing the tests needed for that
area of the specification. This will include details of tests whether or not they are implemented. 
```turtle
  <manifest> doap:implements <https://solidproject.org/TR/protocol> ;
    dcterms:hasPart manifest:group1, manifest:group2, manifest:group3, manifest:group4 .
                    
  manifest:group a td:SpecificationTestCase ;
    td:specificationReference <specref> ;                 
    dcterms:hasPart <feature1> .

  <feature1> a td:TestCase .
```
The `td:SpecificationReference` above should actually be an excerpt:
```turtle
manifest:group td:specificationReference [
  a tn:Excerpt;
  rdfs:seeAlso <specref>;
  tn:includesText "Text from spec"
].
```
##Results
This is the data produced directly as a result of the tests being run.
```turtle
  <feature1> a earl:TestCriterion, earl:TestFeature ;   # from above, this is also a td:TestCase
    earl:assertions <feature1-assertion1> ;
    dcterms:hasPart <feature1-scenario1>, <feature1-scenario2> .

  <feature1-assertion1> a earl:Assertion ;
    earl:assertedBy <harness>;
    earl:test <feature1>;
    earl:subject <ess>;
    earl:mode earl:automatic;
    earl:result <feature1-assertion1-result1> .

  <feature1-assertion1-result1> a earl:TestResult;
    earl:outcome earl:passed;
    dcterms:date "2021-04-06T17:41:20.889+01:00"^^xsd:dateTime .

  <feature1-scenario1> a earl:TestCriterion, earl:TestCase;
    dcterms:isPartOf <feature1>;
    earl:assertions <feature1-scenario1-assertion1>;
    earl:steps ( <step1>, <step2> ) .

  <feature1-scenario1-assertion1> a earl:Assertion ;
    earl:assertedBy <harness>;
    earl:test <feature1-scenario1>;
    earl:subject <ess>;
    earl:mode earl:automatic;
    earl:result <feature1-scenario1-assertion1-result1> .

  <feature1-scenario1-assertion1-result1> a earl:TestResult;
    earl:outcome earl:passed;
    dcterms:date "2021-04-06T17:41:20.889+01:00"^^xsd:dateTime .                  
                          
  <step1> a earl:TestStep;
    dcterms:title "When method PUT";
    earl:outcome earl:passed ;
    earl:info """17:41:19.898 request:
1 > PUT http://localhost:3000/test/31a5b4fb-427d-4238-a67b-32fda7953a0d/53b1a402-5445-4abf-b860-031f6fe6364b/1145deac-8409-4d74-8f45-1d56f6363049.txt
1 > Authorization: DPoP eyJhbGciOiJSUzI1NiIsImtpZCI6IlJTZFhXUEplV0pJIn0.eyJpc3MiOiJodHRwczovL2lucnVwdC5uZXQiLCJhdWQiOiJzb2xpZCIsInN1YiI6Imh0dHBzOi8vc29saWQtdGVzdC1zdWl0ZS1hbGljZS5pbnJ1cHQubmV0L3Byb2ZpbGUvY2FyZCNtZSIsImV4cCI6MTYxODkzNjg3NiwiaWF0IjoxNjE3NzI3Mjc2LCJqdGkiOiIwMzZmNzA0ZmE5YzhhOTU0IiwiY25mIjp7ImprdCI6InhrN1NOUXJ0dWthT0hpaHdIRXI5UEEyR09vQ3U0cWFmVTlDdFlWZXI3cFEifSwiY2xpZW50X2lkIjoiMDY5N2Q1N2E1ZDMwNDQ5ZjhjZmMzMjQxZTQ1OGQ3NjYiLCJ3ZWJpZCI6Imh0dHBzOi8vc29saWQtdGVzdC1zdWl0ZS1hbGljZS5pbnJ1cHQubmV0L3Byb2ZpbGUvY2FyZCNtZSJ9.D3ydNEDlO8pq9EPLb5Ru7Kwi2-918Mbdr7TjYVL8IAP3EQtFAYWCMSk0yet0m_gWNf3B1TrqiOgWiao7kZhOK_ZhNo0O9dX7e8WtYcY7QUokefhdNxEy_KwQEBdBJqnp28vmr4-J5jDNyMpAkWKWJ_UY80pbBzoTO4P3dvnvO8V9pOBZ0pD9Mt-LYxzlIu5RYnkxL6RBjObCMFNKdzX3iiZPMDP656TCWmXC6IlaVDtmhj7-hjPgo8-Lug371Xv68R_EzLnwGzhuf1tP-HO4i4qaIzZe3s42HASMWXv74f1i-42PBHoHPgSLW1JOfVKvzkEMKEpijiAQbzSBsaet7Q
1 > User-Agent: Solid-Conformance-Test-Suite
1 > DPoP: eyJhbGciOiJSUzI1NiIsInR5cCI6ImRwb3Arand0IiwiandrIjp7Imt0eSI6IlJTQSIsImtpZCI6InpnNDdlaVgzR203MSIsInVzZSI6InNpZyIsImFsZyI6IlJTMjU2IiwibiI6Imc0QnZxdGdyR05wcVctM2xLT3lyZXRuMG1iaHdYOXJIQ3YxQlg3bV9GbHBHUXBVblFrci1TdUZONjk2aEpublctQWtuWGEzcE1XY2Izd2VSbVZ5QnA0UTBRZEFDMEFXdGJJcG9VdlF4LUlaX09KejVuRngwNzJIMzBMX0VoV1FNWXo4X0xnaXhsck9WcFJwUlAydXdUR1VaMDk2eTZPX0xtcTlUQmRmRXM1dS1BOXIxdDFMLWg1eWEzMnlJQ3BBSmlOVF9lcTNJOU1uZFloVThtWU9PbGNPOE85Z0JCWlBZVW1QNW4wbWZBY3k3Yko1MlNLdWdhWmluMFQ3STZOcDNPWUtJNlp3dUdiMGJJVnV6dFhldkdTeFRQLTZURzI2TEloOTZSUmJITnFJOWtVRmpXZlMwNi1Ra3FtaDBiRXEwRmhaMlg0Q0ZQUWk5T1E0dVhYdkZnUSIsImUiOiJBUUFCIn19.eyJqdGkiOiJlZTAyZjQ1YS05NGZmLTRiNjYtODg5Ni1jY2RhZmNhMjQyZGYiLCJodG0iOiJQVVQiLCJodHUiOiJodHRwOi8vbG9jYWxob3N0OjMwMDAvdGVzdC8zMWE1YjRmYi00MjdkLTQyMzgtYTY3Yi0zMmZkYTc5NTNhMGQvNTNiMWE0MDItNTQ0NS00YWJmLWI4NjAtMDMxZjZmZTYzNjRiLzExNDVkZWFjLTg0MDktNGQ3NC04ZjQ1LTFkNTZmNjM2MzA0OS50eHQiLCJpYXQiOjE2MTc3MjcyNzl9.CGdNRXczENvNzaxUFHzSllI1tmo2AM4Hhoo50Biwiqo3iqRSBVqmfr4Cs3qQUgtAm8qilFJ0GXsZzPWLjeUasUY05THeL9YrDUsZPohwmKA7esSasSQyWciRWGdtACVc4PY9P0VU9PM501SxdV_R6GDYGAmRKWKN9KTtj_8Nvv8DleWi_alSc7naU1QQayPKBYmt_CI6kjnIJpuapjnl_e9os8IW1y6nP22N_x5O8D18G2eBz5c0MNDJDkDHSeYcRF-jY94CHdjvBe3W1DXKaTsNwWAi9GFlZhQ5kBgrcsfjIXsnb1hlSO_7V-ga0cJVNm8txFj7CQ4alC9A0EURqg
1 > Content-Type: text/plain; charset=UTF-8
1 > Content-Length: 5
1 > Host: localhost:3000
1 > Connection: Keep-Alive
1 > Accept-Encoding: gzip,deflate
Hello

17:41:21.618 response time in milliseconds: 1716
1 < 205
1 < Vary: Accept,Authorization,Origin
1 < X-Powered-By: Community Solid Server
1 < Updates-Via: ws://localhost:3000/
1 < Access-Control-Allow-Origin: *
1 < Access-Control-Allow-Credentials: true
1 < Access-Control-Expose-Headers: Accept-Patch,Location,MS-Author-Via,Updates-Via
1 < Date: Tue, 06 Apr 2021 16:41:21 GMT
1 < Connection: keep-alive
1 < Keep-Alive: timeout=5
1 < Transfer-Encoding: chunked

""" .

  <step2> a earl:TestStep;
    dcterms:title "Then status 200";
    earl:outcome earl:failed ;
    earl:info "Failed" .
```

