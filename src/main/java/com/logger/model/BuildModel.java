package com.logger.model;

public class BuildModel {

    private BUILDSTATUS buildStatus;

    private String buildResponse;

    private String buildNumber;

    public BuildModel(String buildNumber) {
        this.buildNumber = buildNumber;
        buildStatus = BUILDSTATUS.STARTED;
    }

    public BUILDSTATUS getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(BUILDSTATUS buildStatus) {
        this.buildStatus = buildStatus;
    }

    public String getBuildResponse() {
        return buildResponse;
    }

    public void setBuildResponse(String buildResponse) {
        this.buildResponse = buildResponse;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }
}
