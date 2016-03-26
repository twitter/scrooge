package com.twitter;

public class Module
{

    /** Artifact's group id */
    private String groupId;

    /** Artifact's id */
    private String artifactId;

    /**
     * @return id of artifact
     */
    public String getArtifactId()
    {
        return artifactId;
    }

    /**
     * @param artifactId id of artifact
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    /**
     * @return id of artifact's group
     */
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * @param groupId id of artifact's group
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String toString()
    {
        return getGroupId() + ":" + getArtifactId();
    }

}