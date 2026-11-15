package com.readsake.dev.entities;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
public class IngestionJob {
 private UUID id, bookId; private String jobType, sourceKey, status, errorMessage; private int attempts; private Map<String,Object> result; private OffsetDateTime createdAt, startedAt, finishedAt;
 public UUID getId(){return id;} public void setId(UUID v){id=v;} public UUID getBookId(){return bookId;} public void setBookId(UUID v){bookId=v;}
 public String getJobType(){return jobType;} public void setJobType(String v){jobType=v;} public String getSourceKey(){return sourceKey;} public void setSourceKey(String v){sourceKey=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;} public int getAttempts(){return attempts;} public void setAttempts(int v){attempts=v;}
 public Map<String,Object> getResult(){return result;} public void setResult(Map<String,Object> v){result=v;} public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;}
 public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){createdAt=v;} public OffsetDateTime getStartedAt(){return startedAt;} public void setStartedAt(OffsetDateTime v){startedAt=v;} public OffsetDateTime getFinishedAt(){return finishedAt;} public void setFinishedAt(OffsetDateTime v){finishedAt=v;}
}
