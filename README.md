# FB2OnlineConverter

Web application for FictionBook 2.0 ([fb2]) to epub and mobi convertion.

## Features
- download source file file for convertion by URL from remote location
- convert single [fb2] file into [epub] or [mobi] format
- convert multiple files by one request
- email conversion result to user or return it immediately

For fb2 -> epub conversion EPUBGEN from Adobe's [epub-tools] conversion engine used.
For fb2 -> mobi and epub -> mobi conversion Amazon's [KindleGen] converter used.

## Runtime requirements
- java 1.6+
- [Apache Tomcat]
- [Nginx] - (optional)

## Build requirements
- java 1.6+
- [Maven] 3.+


## To build and install converter

- clone project
- execute in project root

    `mvn package`

- copy application webapp/target/converter.war into CATALINA_HOME/webapps
- copy and modify application configuration file webapp/conf/converter.properties into CATALINA_HOME/conf
  - to create and initialize internal database set configuration parameter `database.init = true` before first startup . Don't forget to set it to `false` for subsequent server starts.
- start server and go to http://host:port/converter

Converter can work as standalone application or paired with Nginx as reverse proxy frontend.
Configuration with proxy highly recommended for public installation with high traffic.
Nginx must be compiled with X-accel module (standard).


[Apache Tomcat]: http://tomcat.apache.org/
[Maven]: http://maven.apache.org/
[Nginx]: http://nginx.org/
[fb2]: http://www.gribuser.ru/xml/fictionbook/index.html.en
[epub]: http://idpf.org/epub
[mobi]: http://www.mobipocket.com/dev/article.asp?BaseFolder=prcgen
[epub-tools]: http://code.google.com/p/epub-tools/
[KindleGen]:http://www.amazon.com/gp/feature.html/ref=amb_link_357628042_1?ie=UTF8&docId=1000765211&pf_rd_m=ATVPDKIKX0DER&pf_rd_s=center-6&pf_rd_r=1R29WS3BDDYDA9XG3JRA&pf_rd_t=1401&pf_rd_p=1343256962&pf_rd_i=1000729511
