package isme.service_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    /**
     * Get all projects.
     */
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build(); // Return 204 if no projects found
        }
        return ResponseEntity.ok(projects);
    }

    /**
     * Get a project by its ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProjectById(@PathVariable Long id) {
        Optional<Project> project = projectService.getProjectById(id);
        if (project.isPresent()) {
            return ResponseEntity.ok(project.get());
        } else {
            return ResponseEntity.badRequest().body("Project with ID " + id + " not found.");
        }
    }


    /**
     * Get all projects for a specific user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getProjectsByUserId(@PathVariable Long userId) {
        List<Project> projects = projectService.getProjectsByUserId(userId);
        if (projects.isEmpty()) {
            return ResponseEntity.badRequest().body("No projects found for user ID " + userId);
        }
        return ResponseEntity.ok(projects);
    }

    /**
     * Create a new project.
     */
    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Project project) {
        try {
            Project savedProject = projectService.saveProject(project);
            return ResponseEntity.ok(savedProject);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating project: " + e.getMessage());
        }
    }

    /**
     * Delete a project by its ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        try {
            projectService.deleteProject(id);
            return ResponseEntity.ok("Project with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting project: " + e.getMessage());
        }
    }

    /**
     * Upload a ZIP file and associate it with a project.
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadProjectFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId) {
        try {
            String result = projectService.processUploadedFolder(file, projectId);
            return ResponseEntity.ok("File uploaded and processed successfully:\n" + result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Create a project and upload a ZIP file in a single request.
     */
    @PostMapping("/create-and-upload")
    public ResponseEntity<?> createProjectAndUploadFile(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file) {
        try {
            // Step 1: Create a new project
            Project project = new Project();
            project.setName(name);
            project.setDescription(description);

            Project savedProject = projectService.saveProject(project);

            // Step 2: Process the uploaded file
            String result = projectService.processUploadedFolder(file, savedProject.getId());


            return ResponseEntity.ok("Project created and file uploaded successfully:\n" + result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
