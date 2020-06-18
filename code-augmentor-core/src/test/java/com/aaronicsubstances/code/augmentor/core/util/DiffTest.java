package com.aaronicsubstances.code.augmentor.core.util;

import static org.testng.Assert.*;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DiffTest {

    @Test(dataProvider = "createTestData")
    public void test(List<String> x, List<String> y, String expected) throws Exception {
        StringWriter actualWriter = new StringWriter();
        Diff.printNormalDiff(x, y, actualWriter);
        String actual = actualWriter.toString();
        assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] createTestData() {
        return new Object[][]{
            {
                Arrays.asList("This part of the\r\n", 
                    "document has stayed the\r\n",
                    "same from version to\r\n",
                    "version.  It shouldn't\r\n",
                    "be shown if it doesn't\r\n",
                    "change.  Otherwise, that\r\n",
                    "would not be helping to\r\n",
                    "compress the size of the\r\n",
                    "changes.\r\n",
                    "\r\n",
                    "This paragraph contains\r\n",
                    "text that is outdated.\r\n",
                    "It will be deleted in the\r\n",
                    "near future.\r\n",
                    "\r\n",
                    "It is important to spell\r\n",
                    "check this dokument. On\r\n",
                    "the other hand, a\r\n",
                    "misspelled word isn't\r\n",
                    "the end of the world.\r\n",
                    "Nothing in the rest of\r\n",
                    "this paragraph needs to\r\n",
                    "be changed. Things can\r\n",
                    "be added after it."),
                Arrays.asList("This is an important\r\n",
                    "notice! It should\r\n",
                    "therefore be located at\r\n",
                    "the beginning of this\r\n",
                    "document!\r\n",
                    "\r\n",
                    "This part of the\r\n", 
                    "document has stayed the\r\n",
                    "same from version to\r\n",
                    "version.  It shouldn't\r\n",
                    "be shown if it doesn't\r\n",
                    "change.  Otherwise, that\r\n",
                    "would not be helping to\r\n",
                    "compress the size of the\r\n",
                    "changes.\r\n",
                    "\r\n",
                    "It is important to spell\r\n",
                    "check this document. On\r\n",
                    "the other hand, a\r\n",
                    "misspelled word isn't\r\n",
                    "the end of the world.\r\n",
                    "Nothing in the rest of\r\n",
                    "this paragraph needs to\r\n",
                    "be changed. Things can\r\n",
                    "be added after it.\r\n",
                    "\r\n",
                    "This paragraph contains\r\n",
                    "important new additions\r\n",
                    "to this document."),
                String.format("0a1,6%n" +
                    "> This is an important%n" +
                    "> notice! It should%n" +
                    "> therefore be located at%n" +
                    "> the beginning of this%n" +
                    "> document!%n" +
                    "> %n" +
                    "11,15d16%n" +
                    "< This paragraph contains%n" +
                    "< text that is outdated.%n" +
                    "< It will be deleted in the%n" +
                    "< near future.%n" +
                    "< %n" +
                    "17c18%n" +
                    "< check this dokument. On%n" +
                    "---%n" +
                    "> check this document. On%n" +
                    "24c25,29%n" +
                    "< be added after it.%n" +
                    "\\ No newline at end of file%n" +
                    "---%n" +
                    "> be added after it.%n" +
                    "> %n" +
                    "> This paragraph contains%n" +
                    "> important new additions%n" +
                    "> to this document.%n" +
                    "\\ No newline at end of file%n")
            },
            new Object[]{
                Arrays.asList(""),
                Arrays.asList(""),
                ""
            },
            new Object[]{
                Arrays.asList("my"),
                Arrays.asList("my"),
                ""
            },
            new Object[]{
                Arrays.asList("my\n", "o\n", "mine!\r\n"),
                Arrays.asList("my\n", "o\n", "mine!\r\n"),
                ""
            },
            new Object[]{
                Arrays.asList("The Way that can be told of is not the eternal Way;\n",
                    "The name that can be named is not the eternal name.\n",
                    "The Nameless is the origin of Heaven and Earth;\n",
                    "The Named is the mother of all things.\n",
                    "Therefore let there always be non-being,\n",
                    "  so we may see their subtlety,\n",
                    "And let there always be being,\n",
                    "  so we may see their outcome.\n",
                    "The two are the same,\n",
                    "But after they are produced,\n",
                    "  they have different names.\n"),
                Arrays.asList("The Nameless is the origin of Heaven and Earth;\n",
                    "The named is the mother of all things.\n",
                    "\n",
                    "Therefore let there always be non-being,\n",
                    "  so we may see their subtlety,\n",
                    "And let there always be being,\n",
                    "  so we may see their outcome.\n",
                    "The two are the same,\n",
                    "But after they are produced,\n",
                    "  they have different names.\n",
                    "They both may be called deep and profound.\n",
                    "Deeper and more profound,\n",
                    "The door of all subtleties!\n"),
                String.format("1,2d0%n" +
                    "< The Way that can be told of is not the eternal Way;%n" +
                    "< The name that can be named is not the eternal name.%n" +
                    "4c2,3%n" +
                    "< The Named is the mother of all things.%n" +
                    "---%n" +
                    "> The named is the mother of all things.%n" +
                    "> %n" +
                    "11a11,13%n" +
                    "> They both may be called deep and profound.%n" +
                    "> Deeper and more profound,%n" +
                    "> The door of all subtleties!%n")
            }
        };
    }
}