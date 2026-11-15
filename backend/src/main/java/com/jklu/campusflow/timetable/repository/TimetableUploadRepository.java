package com.jklu.campusflow.timetable.repository;

import com.jklu.campusflow.timetable.model.TimetableUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TimetableUploadRepository extends JpaRepository<TimetableUpload, UUID> {
}