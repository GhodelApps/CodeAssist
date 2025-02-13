package com.tyron.code.ui.editor.language.xml;

import com.tyron.code.ui.editor.language.Language;
import com.tyron.editor.Editor;

import java.io.File;


public class Xml implements Language {
	
	@Override
	public boolean isApplicable(File file) {
		return file.getName().endsWith(".xml");
	}
	
	@Override
	public io.github.rosemoe.sora.lang.Language get(Editor editor) {
		return new LanguageXML(editor);
	}
}
