package isme.service_project;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepo projectRepository;

    /**
     * Get all projects.
     */
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * Get a project by ID.
     */
    public Optional<Project> getProjectById(Long id) {
        return projectRepository.findById(id);
    }
    public List<Project> getProjectsByUserId(Long userId) {
        return projectRepository.findByUserId(userId);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }


    /**
     * Save a new project or update an existing one.
     */
    public Project saveProject(Project project) {
        System.out.println("Saving project: " + project.getName());
        return projectRepository.save(project);
    }

    /**
     * Process and store an uploaded folder (ZIP file).
     */
    public String processUploadedFolder(MultipartFile file, Long projectId) throws Exception {
        // Récupérer le projet par ID
        Project project = getProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Project with ID " + projectId + " not found."));

        // Chemin d'upload
        String uploadDir = System.getProperty("user.dir") + "/uploads/" + projectId;
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Enregistrer le fichier ZIP
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        Path zipFilePath = uploadPath.resolve(originalFilename);
        file.transferTo(zipFilePath.toFile());

        // Extraire les fichiers
        Path extractionDir = uploadPath.resolve("extracted");
        unzipFile(zipFilePath.toString(), extractionDir.toString());

        // Lire le contenu des fichiers extraits
        StringBuilder analysisResult = new StringBuilder("Folder analysis result:\n");
        Files.walk(extractionDir)
                .filter(Files::isRegularFile)
                .forEach(extractedFile -> {
                    try {
                        byte[] content = Files.readAllBytes(extractedFile);
                        analysisResult.append("Found file: ").append(extractedFile.getFileName()).append("\n");

                        // Mise à jour du projet avec le contenu
                        project.setFileName(extractedFile.getFileName().toString());
                        project.setFileContent(content); // Enregistrer le contenu
                        project.setFileType(Files.probeContentType(extractedFile));
                        project.setUploadTime(LocalDateTime.now());
                        projectRepository.save(project);

                    } catch (Exception e) {
                        analysisResult.append("Error reading file: ").append(extractedFile.getFileName()).append("\n");
                    }
                });

        return analysisResult.toString();
    }



    /**
     * Extracts a ZIP file to a specified directory.
     */
    private void unzipFile(String zipFilePath, String extractionPath) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = Paths.get(extractionPath, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath); // Create directories for folders
                } else {
                    Files.createDirectories(filePath.getParent()); // Ensure parent directories exist

                    // Write the file contents
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during ZIP extraction: " + e.getMessage(), e);
        }
    }

    /**
     * Analyze the contents of a folder.
     */
    private String analyzeFolder(String folderPath) {
        StringBuilder analysisResult = new StringBuilder("Folder analysis result:\n");
        try {
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> analysisResult.append("Found file: ").append(file.getFileName()).append("\n"));
        } catch (Exception e) {
            analysisResult.append("Error during folder analysis: ").append(e.getMessage());
        }
        return analysisResult.toString();
    }
}

