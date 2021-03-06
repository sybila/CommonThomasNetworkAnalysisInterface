package mu.fi.sybila.esther.behaviourmapwidget.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.annotation.Resource;
import javax.sql.DataSource;
import mu.fi.sybila.esther.heart.controllers.FileSystemController;
import mu.fi.sybila.esther.heart.database.FileSystemManager;
import mu.fi.sybila.esther.heart.database.TaskManager;
import mu.fi.sybila.esther.heart.database.UserManager;
import mu.fi.sybila.esther.heart.database.entities.EstherFile;
import mu.fi.sybila.esther.heart.database.entities.Task;
import mu.fi.sybila.esther.heart.database.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for behaviour mapping.
 * 
 * @author George Kolcak
 */
@Controller
public class BehaviourMapController
{
    private FileSystemManager fileSystemManager = new FileSystemManager();
    private UserManager userManager = new UserManager();
    private TaskManager taskManager = new TaskManager();
    public static final Logger logger = Logger.getLogger(BehaviourMapController.class.getName());
    
    @Value("${allowed_parallel_tasks}")
    private Integer maxTasks;
    
    @Value("${behaviour_mapper_location}")
    private String behaviourMapperLocation;
    
    @Autowired
    private FileSystemController fileSystemController;
    
    /**
     * Method for setting up the controller.
     * 
     * @param dataSource The DataSource used for database connection.
     */
    @Resource
    public void setDataSource(DataSource dataSource)
    {
        fileSystemManager.setDataSource(dataSource);
        taskManager.setDataSource(dataSource);
        userManager.setDataSource(dataSource);
    }
    
    /**
     * Method for setting up the controller.
     * 
     * @param dataLocation The location of the data in the local file system.
     */
    @Value("${data_location}")
    public void setDataLocation(String dataLocation)
    {
        fileSystemManager.setDataLocation(dataLocation);
    }
    
    /**
     * Method for setting up the controller.
     * 
     * @param fs The output stream for logger output.
     */
    public void setLogger(FileOutputStream fs)
    {
        logger.addHandler(new StreamHandler(fs, new SimpleFormatter()));
        fileSystemManager.setLogger(fs);
        taskManager.setLogger(fs);
        userManager.setLogger(fs);
    }
    
    /**
     * Handler method for behaviour map creation.
     * 
     * @param sourceId The ID of the parameter set files to use as an input.
     * @param filterId The ID of the filter file to restrain the output with.
     * @return The ID of the created behaviour map file.
     *         Error message if behaviour map creation fails.
     *         Limit reached message if the behaviour map exceeds the allowed storage space.
     */
    @RequestMapping(value = "Widget/BehaviourMap", method = RequestMethod.POST)
    @ResponseBody
    public String generateBehaviourMap(@RequestParam("file") Long sourceId,
        @RequestParam(value = "filter", required = false) Long filterId)
    {
        if (sourceId == null)
        {
            return "ERROR=Invalid data specified.";
        }
        
        EstherFile source = fileSystemManager.getFileById(sourceId);
        File file = fileSystemManager.getSystemFileById(sourceId);
        
        EstherFile parentProperty = fileSystemManager.getParent(source);
        EstherFile parentModel = fileSystemManager.getParent(parentProperty);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        User user = userManager.getUserByUsername(authentication.getName());
        
        if (!authentication.isAuthenticated() || (user == null))
        {
            return "ERROR=You are not loggen in.";
        }
        
        if (taskManager.getActiveTaskCount(user) >= maxTasks)
        {
            return "LIMIT_REACHED=" + maxTasks;
        }
        
        if (user.getId() != source.getOwner())
        {
            return "ERROR=Cannot create Behaviour Map from public file you do not own.";
        }
        
        Long targetId;
        
        File filterFile = null;
        
        if (filterId != null)
        {
            EstherFile filter = fileSystemManager.getFileById(filterId);

            if (user.getId() != filter.getOwner())
            {
                return "ERROR=You cannot use public filter you do not own to create Behaviour Map.";
            }

            filterFile = fileSystemManager.getSystemFileById(filter.getId());

            targetId = Long.parseLong(fileSystemController.createFile(filter.getName(), "xgmml", filter.getId(), true));
        }
        else
        {
            targetId = Long.parseLong(fileSystemController.createFile(source.getName(), "xgmml", source.getId(), true));
        }
        
        File outputFile = fileSystemManager.getSystemFileById(targetId);
        
        Task task = Task.newTask(user.getId(), parentModel.getId(), parentProperty.getId(), targetId, "behaviour_mapper");
        
        task.addDatabase(source.getId());
        
        if (filterId != null)
        {
            task.addFilter(filterId);
        }

        List<String> taskArgs = new ArrayList<>();

        taskArgs.add("java");
        taskArgs.add("-jar");
        taskArgs.add(behaviourMapperLocation);
        taskArgs.add(file.getAbsolutePath());
        taskArgs.add(outputFile.getAbsolutePath());
        
        if (filterFile != null)
        {
            taskArgs.add(filterFile.getAbsolutePath());
        }
        
        taskArgs.add("-p");
        taskArgs.add(parentProperty.getId().toString());
        
        Long taskId = taskManager.createTask(task, taskArgs.toArray(new String[] { }));
        
        return taskId.toString();
    }
}
