package com.example.codex;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

import com.amrdeveloper.codeview.CodeView;

import java.util.regex.Pattern;

public class Code_example extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_example);

        WebView webView = findViewById(R.id.codeWebView);
        webView.getSettings().setJavaScriptEnabled(true);

        String code =
                "public class Main {\n" +
                        "    public static void main(String[] args) {\n" +
                        "        System.out.println(\"Hello World\");\n" +
                        "    }\n" +
                        "}";

        String html = "<html><head>" +
                "<link href=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/themes/prism.min.css\" rel=\"stylesheet\" />" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/prism.min.js\"></script>" +
                "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/prism/1.29.0/components/prism-java.min.js\"></script>" +
                "</head>" +
                "<body style='background:#FFFFFF; margin:0; padding:12px;'>" +

                "<pre style='font-size:14px; font-weight:600; font-family:monospace;'>" +   // ✅ Semi-bold
                "<code class='language-java'>" +
                code +
                "</code></pre>" +
                "</body></html>";

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
}
