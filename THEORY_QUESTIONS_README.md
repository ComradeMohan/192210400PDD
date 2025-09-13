# Theory Questions Feature

This document describes the theory questions feature added to the UniValut app.

## Overview

The theory questions feature allows students to answer 15 descriptive questions for each course. Unlike MCQ tests, these questions require detailed written answers with no time limit.

## Features

- **15 Questions**: Each test contains exactly 15 theory-based questions
- **No Time Limit**: Students can take as much time as needed
- **Text Input**: Students provide detailed written answers
- **Progress Tracking**: Visual progress bar shows completion status
- **Navigation**: Previous/Next buttons to navigate between questions
- **Auto-save**: Answers are saved as students navigate between questions
- **Submission**: Final submission with confirmation dialog

## Files Added/Modified

### Android Files

1. **PrepActivity.kt** - Added theory questions card and navigation
2. **TheoryQuestionActivity.kt** - New activity for theory questions
3. **activity_prep.xml** - Added theory questions card to layout
4. **activity_theory_questions.xml** - New layout for theory questions
5. **AndroidManifest.xml** - Registered TheoryQuestionActivity

### Drawable Resources

1. **rounded_bg_light_red.xml** - Red background for theory questions card
2. **rounded_edittext_background.xml** - Background for answer input field
3. **rounded_button_background.xml** - Background for navigation buttons

### PHP API Files

1. **get_theory_questions.php** - API to fetch theory questions
2. **submit_theory_answers.php** - API to submit student answers
3. **create_theory_questions_table.sql** - Database schema and sample data
4. **setup_theory_questions.php** - Setup script for database tables

## Database Schema

### Tables Created

1. **theory_questions** - Stores theory questions
   - question_id (Primary Key)
   - course_id (Foreign Key)
   - question_text
   - sample_answer
   - difficulty_level (EASY, MEDIUM, HARD)
   - created_at

2. **theory_test_results** - Stores test session information
   - test_result_id (Primary Key)
   - student_id
   - course_id
   - total_questions
   - test_date

3. **theory_test_answers** - Stores individual answers
   - answer_id (Primary Key)
   - test_result_id (Foreign Key)
   - question_id
   - student_answer

## Setup Instructions

1. **Database Setup**:
   ```bash
   php setup_theory_questions.php
   ```

2. **API Configuration**:
   - Update database credentials in PHP files if needed
   - Ensure web server can access the PHP files

3. **Android App**:
   - Build and install the updated app
   - The theory questions card will appear in PrepActivity

## Usage

1. **Student Flow**:
   - Open a course in PrepActivity
   - Tap on "Theory Questions" card
   - Answer 15 questions with detailed explanations
   - Navigate using Previous/Next buttons
   - Submit when complete

2. **Features**:
   - Questions are randomized for each session
   - Answers are auto-saved during navigation
   - Progress bar shows completion status
   - Confirmation dialog before submission

## API Endpoints

### Get Theory Questions
- **URL**: `get_theory_questions.php`
- **Method**: GET
- **Parameters**:
  - `course_id` (required): Course ID
  - `limit` (optional): Number of questions (default: 15)
  - `difficulty` (optional): EASY, MEDIUM, HARD

### Submit Theory Answers
- **URL**: `submit_theory_answers.php`
- **Method**: POST
- **Body**: JSON with student_id, course_id, and answers array

## Sample Questions

The system includes 15 sample theory questions for Artificial Intelligence course covering:
- AI fundamentals and approaches
- Machine learning concepts
- Neural networks
- Natural language processing
- Computer vision
- Expert systems
- Search algorithms
- Ethics in AI

## Customization

- **Add More Questions**: Insert new questions into `theory_questions` table
- **Different Courses**: Add questions with appropriate `course_id`
- **Difficulty Levels**: Use EASY, MEDIUM, HARD for question classification
- **UI Styling**: Modify drawable resources for different appearance

## Notes

- Questions are fetched randomly for each session
- No time limit encourages thoughtful answers
- Answers are stored as plain text
- System supports multiple courses with different question sets
- Database uses transactions for data integrity
