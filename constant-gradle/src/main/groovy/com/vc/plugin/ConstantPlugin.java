package com.vc.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ConstantPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().create("constant", Params.class);
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                Params params = (Params) project.getExtensions().getByName("constant");
                params.createClass(project);
            }
        });
    }
}
