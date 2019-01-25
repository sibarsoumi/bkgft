# Building Knowledge Graph from Tables (bkgft)
### How to run 
Given a wikipedia dump as a file containing wikipedia articles in the format `<link><article>\n<link><article>\n ... <link><article>\n`.
* Running `java LinksExtractor pathOfDump pathOfOutput` extracts all the links in the given dump and saves them in a file in the given pathOfOutput.
* Running `java Reader pathOfURLList pathOfOutput` iterates over the given URLs, fetches the tables in each article, preprocesses them and saves them as JSON Objects in the given pathOfOutput.
* Running `java GraphGenerator pathOfTables pathOfOutput` reads the tables in the given pathOfTables, extracts facts from the given tables and stores them in an RDF repository in the given pathOfOutput.
* Running `java RDFRepoToTTLConverter pathOfRepo pathOfOutput` converts the facts in the given repository to RDF statements in turtle format and saves them in a file in the given pathOfOutput.
* Running `java RandomlyChooser pathOfInputTTLFile pathOfOutputTTLFile n` chooses n statements randomly from the given pathOfInputTTLFile and stores them in a new file in pathOfOutputTTLFile (excluding statements having the predicate rdfs:label).
### Example
Given a wikipedia dump in: `E:\enwiki-20171103-pages.tsv`
1. After running `java LinksExtractor E:\enwiki-20171103-pages.tsv E:\list` a list of URLs of the given articles is in E:\list
2. After running `java Reader E:\list E:\tables` fetched tables are stored (JSON encoded) in E:\tables (each article corresponds one file)
3. After running `java GraphGenerator E:\tables E:\repo` a repository is generated in E:\repo (using RDF4J) and facts from the tables are added to it.
4. After running `java RDFRepoToTTLConverter E:\repo E:\graph.ttl` the generated statements are stored in RDF turtle format in E:\graph.ttl.
5. After running `java RandomlyChooser E:\graph.ttl E:\chosen.ttl 200` 200 statements (excluding statements having the predicate rdfs:label) are randomly chosen and stored in E:\chosen.ttl.
