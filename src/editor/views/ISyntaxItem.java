package editor.views;

public interface ISyntaxItem {

	ISyntaxItem[] NONE = null;

	boolean isSyntaxFor(Object obj);

	String getInfo();

}
