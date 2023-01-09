package util;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FilterPhraseExplanations {

    static class PhraseExplanations {
        final String phrase;
        final String pinyin;
        final String[] explanations;

        public PhraseExplanations(String phrase, String pinyin, String[] explanations) {
            this.phrase = phrase;
            this.pinyin = pinyin;
            this.explanations = explanations;
        }
    }

    public static final String OUTPUT_FILE_NAME = "phrases.json";
    public static final String TAG_HORIZONTAL_LINE = "hr";
    public static final String CLASS_EXAMPLE = "fontcyan";
    public static final String CLASS_ENGLISH = "teal";

    public static void main(String[] args) {
        if (1 > args.length) {
            System.err.println("Usage: <file1> [<file2> [, <file3> [, ...]]]");
            return;
        }
        List<PhraseExplanations> phrases = new ArrayList<>();
        for (String file_path: args) {
            System.out.println("Parsing file: " + file_path);
            long time_start = System.currentTimeMillis();
            filter(new File(file_path), phrases);
            long time_done = System.currentTimeMillis();
            System.out.printf("Done (%d ms)\n", time_done - time_start);
        }
        System.out.println("Dumping ...");
        try (FileWriter writer = new FileWriter(OUTPUT_FILE_NAME)) {
            new Gson().toJson(phrases, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * CSV format: <词汇> <拼音> <解释源码（HTML）>
     */
    static void filter(File file, List<PhraseExplanations> phrases) {
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build()) {
            String[] line;
            while (null != (line = reader.readNext())) {
                phrases.add(new PhraseExplanations(line[0], line[1], parseExplanations(line[2])));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    static String[] parseExplanations(String code) {
        List<String> explanations = new ArrayList<>();
        Document doc = Jsoup.parse(code);
        doc.body().select("." + CLASS_ENGLISH).remove();
        doc.body().select("." + CLASS_EXAMPLE).remove();
        for (Element e: doc.body().children()) {
            if (TAG_HORIZONTAL_LINE.equals(e.tag().getName())) {
                break;
            }
            String text = e.text().strip();
            if (!text.isEmpty()) {
                /* Format explanation */
                String[] sub_items = text.split("\\(\\d+\\)|\\d+[、.]");    // split in-line numberings
                for (String sub_item: sub_items) {
                    if(!sub_item.isEmpty()) {
                        sub_item = sub_item.replaceFirst("(例|如|例如)：.+$", "").strip();  // remove in-line examples
                        if (!sub_item.isEmpty()) {
                            explanations.add(sub_item);
                        }
                    }
                }
            }
        }
        return explanations.toArray(new String[0]);
    }
}
