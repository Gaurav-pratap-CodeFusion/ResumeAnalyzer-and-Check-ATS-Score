package gpcf.resumeanalyzer.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gpcf.resumeanalyzer.Service.GroqService;
import org.apache.tika.Tika;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/resume")
public class GroqController {

    private final GroqService groqService;
    private final Tika tika = new Tika();
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqController(GroqService groqService) {
        this.groqService = groqService;
    }

    private String limitText(String text) {
        int MAX = 12000;
        return text.length() > MAX ? text.substring(0, MAX) : text;
    }

    private String cleanJson(String aiResponse) {
        return aiResponse
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();
    }

    @PostMapping(
            value = "/analyzer",
            consumes = "multipart/form-data",
            produces = "application/json"
    )
    public Map<String, Object> analyze(@RequestParam("file") MultipartFile file) throws Exception {

        String content = limitText(tika.parseToString(file.getInputStream()));

        String prompt = """
                You are a professional technical resume reviewer.
                Return ONLY valid JSON. No markdown. No explanations.
                
                Resume:
                %s
                
                Output JSON format:
                {
                  "skills": [],
                  "qualityScore": 0,
                  "improvements": []
                }
                """.formatted(content);

        String aiResponse = groqService.chat(prompt, "llama-3.1-8b-instant");

        String clean = cleanJson(aiResponse);

        return mapper.readValue(clean, new TypeReference<>() {
        });
    }

    @PostMapping(
            value = "/ats-check",
            consumes = "multipart/form-data",
            produces = "application/json"
    )
    public Map<String, Object> checkATS(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jd") String jobDescription) throws Exception {

        String resumeText = limitText(tika.parseToString(file.getInputStream()));

        String prompt = """
                You are an ATS analyzer.
                Return ONLY valid JSON.
                
                Resume:
                %s
                
                Job Description:
                %s
                
                Output JSON format:
                {
                  "atsScore": (0-100),
                  "matchedKeywords": [],
                  "missingKeywords": [],
                  "summary": ""
                }
                """.formatted(resumeText, jobDescription);

        String aiResponse = groqService.chat(prompt, "llama-3.1-8b-instant");

        String clean = cleanJson(aiResponse);

        return mapper.readValue(clean, new TypeReference<>() {
        });
    }
}
