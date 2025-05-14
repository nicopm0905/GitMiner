package aiss.bitbucketminer.service;

import aiss.bitbucketminer.etl.Transformer;
import aiss.bitbucketminer.model.bitbucket.BitBucketComment;
import aiss.bitbucketminer.model.bitbucket.BitBucketIssue;
import aiss.bitbucketminer.model.gitminer.Comment;
import aiss.bitbucketminer.model.gitminer.Issue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IssueService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gitminer.url}")
    private String gitminerUrl;

    public List<Issue> fetchIssuesFromBitbucket(String workspace, String repoSlug, int nIssues, int maxPages) {
        List<Issue> result = new ArrayList<>();

        String baseUrl = String.format(
                "https://api.bitbucket.org/2.0/repositories/%s/%s/issues?pagelen=%d",
                workspace, repoSlug, nIssues
        );

        String nextPage = baseUrl;
        int page = 0;

        while (nextPage != null && page < maxPages) {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    nextPage,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawIssues = (List<Map<String, Object>>) response.getBody().get("values");

            for (Map<String, Object> raw : rawIssues) {
                Integer issueId = (Integer) raw.get("id");

                // Obtenemos el issue completo
                BitBucketIssue bitbucketIssue = restTemplate.getForObject(
                        "https://api.bitbucket.org/2.0/repositories/" + workspace + "/" + repoSlug + "/issues/" + issueId,
                        BitBucketIssue.class
                );

                Issue issue = Transformer.toGitMinerIssue(bitbucketIssue);

                // Añadimos los comments
                List<Comment> comments = fetchCommentsForIssue(workspace, repoSlug, issueId);
                issue.setComments(comments);

                result.add(issue);
            }


            nextPage = (String) response.getBody().get("next");
            page++;
        }

        return result;
    }

    public void sendIssuesToGitMiner(List<Issue> issues) {
        for (Issue issue : issues) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Issue> request = new HttpEntity<>(issue, headers);
            restTemplate.postForEntity(gitminerUrl + "/issues", request, Void.class);
        }
    }

    public void fetchAndSend(String workspace, String repoSlug, int nIssues, int maxPages) {
        List<Issue> issues = fetchIssuesFromBitbucket(workspace, repoSlug, nIssues, maxPages);
        sendIssuesToGitMiner(issues);
    }

    public List<Comment> fetchCommentsForIssue(String workspace, String repoSlug, Integer issueId) {
        String url = String.format(
                "https://api.bitbucket.org/2.0/repositories/%s/%s/issues/%d/comments",
                workspace, repoSlug, issueId
        );

        List<Comment> comments = new ArrayList<>();

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawComments = (List<Map<String, Object>>) response.getBody().get("values");

        if (rawComments != null) {
            for (Map<String, Object> raw : rawComments) {
                Integer commentId = (Integer) raw.get("id");

                BitBucketComment bitbucketComment = restTemplate.getForObject(
                        url + "/" + commentId,
                        BitBucketComment.class
                );

                Comment comment = Transformer.toGitMinerComment(bitbucketComment);
                comments.add(comment);
            }
        }

        return comments;
    }

}

