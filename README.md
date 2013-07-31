Json2Java
=========

I was fed up with writing Java classes to mirror json models.  So I wrote this Java app to automate the process.

<h3>What this tool can do right now:</h3>

when run from the commandline, it takes three arguments:
<ul>
<li>a url to a json file</li>
<li>a package name for the classes it will generate</li>
<li>the name of the base class to start with.</li>
</ul>
e.g. java -jar JsonToJava http://example.com/folder.json com.example.api.model Folder

It will create the folder structure for the package you provide, then it will read the json at the url you provide, and output java classes into the folder locally.

Each class will implement Parcelable for easy passing of information in Android.
Each class will have a default empty constructor.
The members of each class will follow Android naming conventions with an "m" prefix and camel case.
Each member will have a corresponding static final String that relates it to it's json counterpart
Each member will have a @SerializedName annotation for very easy Gson parsing.
If a member called "mId" or "mUniqueId" is found, then equals and hashcode will be overridden so comparisons are made on the id.

<h3>What I want this tool to do in the future in no particular order</h3>
<ul>
<li>Work as an App Engine app</li>
<li>Accept either a url or a block of json pasted in a text box</li>
<li>Extract superclasses</li>
<li>Accept multiple urls to build up the whole model</li>
<li>Allow the user to choose between Gson annotations, Jackson annotations, org.json constructors and toJson() methods, or none of the above</li>
<li>Give users the ability to rename classes</li>
<li>Give users the ability to ignore classes</li>
</ul>
