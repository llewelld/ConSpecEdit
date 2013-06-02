This project implements a ConSpec file editor and new file wizard as an Eclipse plugin.

It has been created as part of the Aniketos project: http://aniketos.eu/

The ConSpec XML marshalling/unmarshalling class structure was generated using JAXB directly from the XML Schema. The schema has been altered slightly for clarity. In addition, one more substantial change was needed in order to work effectively with JAXB. This was to alter binary relations from using a sequence of identical types, to a sequence containing a type requiring two occurences.

From:

<xs:sequence>
	<xs:group ref="expType"/>
	<xs:group ref="expType"/>
</xs:sequence>

To:
			
<xs:sequence>
	<xs:group ref="expType" minOccurs="2" maxOccurs="2"/>
</xs:sequence>

It's not clear to me whether this actually impacts on validation (it didn't appear to).

The included ant build file will regenerate the JAXB classes using XJCTask. Once generated, the project will compile and run in Eclipse as usual.

JAXB 2.2.6 is needed and should be placed in the same workspace folder as the project (i.e. at the same level as the project folder).

http://jaxb.java.net/

Any problems, please let me know:

David Llewellyn-Jones, 11/11/2012, D.Llewellyn-Jones@ljmu.ac.uk
