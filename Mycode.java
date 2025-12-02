import React, { useState, useEffect } from 'react';
import { Briefcase, Play, RotateCcw, Mic, Clock, CheckCircle, History, TrendingUp, Award } from 'lucide-react';

export default function MockInterviewApp() {
  const [currentScreen, setCurrentScreen] = useState('login');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [signupName, setSignupName] = useState('');
  const [signupEmail, setSignupEmail] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [signupConfirmPassword, setSignupConfirmPassword] = useState('');
  const [authError, setAuthError] = useState('');
  const [hasSavedProgress, setHasSavedProgress] = useState(false);
  const [jobRole, setJobRole] = useState('');
  const [jobLevel, setJobLevel] = useState('entry');
  const [difficulty, setDifficulty] = useState('medium');
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [userAnswer, setUserAnswer] = useState('');
  const [answers, setAnswers] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [sessionTime, setSessionTime] = useState(0);
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const [interviewHistory, setInterviewHistory] = useState([]);
  const [overallScore, setOverallScore] = useState(0);
  const [gradeFeedback, setGradeFeedback] = useState(null);
  const [isLoadingFeedback, setIsLoadingFeedback] = useState(false);
  const [interviewQuestions, setInterviewQuestions] = useState([]);
  const [isLoadingQuestions, setIsLoadingQuestions] = useState(false);


  // Check authentication on mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const result = await window.storage.get('current-user', false);
        if (result && result.value) {
          const user = JSON.parse(result.value);
          setCurrentUser(user);
          setIsAuthenticated(true);
          setCurrentScreen('home');
          await loadUserHistory(user.email);
          
          // Check for saved progress
          const progress = await loadInterviewProgress(user.email);
          if (progress) {
            setHasSavedProgress(true);
          }
        }
      } catch (error) {
        console.log('No authenticated user:', error);
      }
    };
    checkAuth();
  }, []);

  // Timer effect
  useEffect(() => {
    let interval;
    if (isTimerRunning) {
      interval = setInterval(() => {
        setSessionTime(prev => prev + 1);
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [isTimerRunning]);

  // Load history from storage
  const loadUserHistory = async (userEmail) => {
    try {
      const result = await window.storage.get(`interview-history-${userEmail}`, false);
      if (result && result.value) {
        setInterviewHistory(JSON.parse(result.value));
      }
    } catch (error) {
      console.log('No history found:', error);
    }
  };

  useEffect(() => {
    if (isAuthenticated && currentUser) {
      loadUserHistory(currentUser.email);
    }
  }, [isAuthenticated, currentUser]);

  const handleLogin = async () => {
    setAuthError('');
    
    if (!loginEmail || !loginPassword) {
      setAuthError('Please fill in all fields');
      return;
    }

    try {
      let result;
      try {
        result = await window.storage.get(`user-${loginEmail}`, false);
      } catch (e) {
        setAuthError('Account not found. Please sign up first.');
        return;
      }

      if (!result || !result.value) {
        setAuthError('Account not found. Please sign up first.');
        return;
      }

      const user = JSON.parse(result.value);
      if (user.password !== loginPassword) {
        setAuthError('Incorrect password');
        return;
      }

      setCurrentUser(user);
      setIsAuthenticated(true);
      await window.storage.set('current-user', JSON.stringify(user), false);
      setCurrentScreen('home');
      setLoginEmail('');
      setLoginPassword('');
      await loadUserHistory(user.email);
      
      // Check for saved progress
      const progress = await loadInterviewProgress(user.email);
      if (progress) {
        setHasSavedProgress(true);
      }
    } catch (error) {
      console.error('Login error:', error);
      setAuthError('Login failed. Please try again.');
    }
  };

  const handleSignup = async () => {
    setAuthError('');

    if (!signupName || !signupEmail || !signupPassword || !signupConfirmPassword) {
      setAuthError('Please fill in all fields');
      return;
    }

    if (signupPassword !== signupConfirmPassword) {
      setAuthError('Passwords do not match');
      return;
    }

    if (signupPassword.length < 6) {
      setAuthError('Password must be at least 6 characters');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(signupEmail)) {
      setAuthError('Please enter a valid email address');
      return;
    }

    try {
      // Check if user exists
      let existingUser;
      try {
        existingUser = await window.storage.get(`user-${signupEmail}`, false);
      } catch (e) {
        // User doesn't exist, which is what we want
        existingUser = null;
      }

      if (existingUser && existingUser.value) {
        setAuthError('An account with this email already exists');
        return;
      }

      const newUser = {
        name: signupName,
        email: signupEmail,
        password: signupPassword,
        createdAt: new Date().toISOString()
      };

      // Store user data
      const userResult = await window.storage.set(`user-${signupEmail}`, JSON.stringify(newUser), false);
      if (!userResult) {
        throw new Error('Failed to create account');
      }

      const sessionResult = await window.storage.set('current-user', JSON.stringify(newUser), false);
      if (!sessionResult) {
        throw new Error('Failed to create session');
      }
      
      setCurrentUser(newUser);
      setIsAuthenticated(true);
      setCurrentScreen('home');
      setSignupName('');
      setSignupEmail('');
      setSignupPassword('');
      setSignupConfirmPassword('');
    } catch (error) {
      console.error('Signup error:', error);
      setAuthError('Signup failed. Please try again or use a different email.');
    }
  };

  const handleLogout = async () => {
    try {
      await window.storage.delete('current-user', false);
    } catch (error) {
      console.error('Logout error:', error);
    }
    setCurrentUser(null);
    setIsAuthenticated(false);
    setCurrentScreen('login');
    setInterviewHistory([]);
    setLoginEmail('');
    setLoginPassword('');
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const generateInterviewQuestions = async (jobRole, jobLevel, difficulty) => {
    setIsLoadingQuestions(true);
    try {
      const difficultyDescriptions = {
        easy: 'basic and straightforward',
        medium: 'moderately challenging with some behavioral questions',
        hard: 'challenging with complex scenarios and situational questions'
      };

      const levelDescriptions = {
        entry: 'entry-level (0-2 years experience)',
        mid: 'mid-level (3-7 years experience)',
        senior: 'senior-level (8+ years experience)'
      };

      const difficultyInstructions = {
        easy: `Make questions in-depth and comprehensive, focusing on:
- Fundamental knowledge and core skills required for ${jobRole}
- Basic situational scenarios they might encounter
- Their understanding of key tools, processes, and methodologies
- Past experiences demonstrating foundational competencies
- How they approach learning and development in this field
Even though this is "easy" difficulty, questions should still be detailed and require thoughtful, substantial answers.`,
        medium: `Make questions thoroughly in-depth and detailed, focusing on:
- Advanced application of skills and knowledge specific to ${jobRole}
- Complex scenarios requiring problem-solving and critical thinking
- Multi-faceted challenges that test various competencies simultaneously
- Leadership, collaboration, and stakeholder management situations
- Strategic thinking and decision-making processes
- Detailed technical or functional expertise
Questions should require comprehensive answers with specific examples and deep reflection.`,
        hard: `Make questions extremely in-depth and challenging, focusing on:
- Highly complex, multi-layered scenarios with competing priorities
- Strategic decision-making under pressure with incomplete information
- Advanced expertise in specialized areas of ${jobRole}
- Organizational impact and transformational change
- Handling crisis situations and difficult stakeholder dynamics
- Innovation, thought leadership, and industry expertise
- Ethical dilemmas and controversial decisions
Questions should push candidates to demonstrate mastery-level thinking and require extensive, nuanced responses.`
      };

      const prompt = `You are an experienced hiring manager conducting an interview for a ${jobRole} position at the ${levelDescriptions[jobLevel]} level.

First, think about the key requirements, skills, and responsibilities typically needed for a ${jobRole} role at this level. Then generate exactly 10 highly specific, in-depth interview questions that directly assess whether the candidate meets those requirements.

Difficulty Level Instructions:
${difficultyInstructions[difficulty]}

Requirements:
- Generate exactly 10 questions (not 5, not 8 - must be 10)
- Each question must be tailored specifically to ${jobRole} - use industry terminology, tools, methodologies, and scenarios unique to this role
- Questions should be detailed and require comprehensive answers (candidates should speak for 2-3 minutes per answer)
- Ask about specific tools, technologies, processes, or challenges relevant to ${jobRole}
- Include questions that reveal hands-on experience with core responsibilities
- Progress from foundational to more complex throughout the 10 questions
- Mix: behavioral (past experience), technical (skills/knowledge), and situational (problem-solving) questions
- Avoid generic questions that could apply to any job
- Make questions thought-provoking and substantial

Return ONLY the 10 questions, one per line, numbered 1-10. No preamble or explanations.

Format:
1. [Question here]
2. [Question here]
3. [Question here]
4. [Question here]
5. [Question here]
6. [Question here]
7. [Question here]
8. [Question here]
9. [Question here]
10. [Question here]`;

      const response = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 1000,
          messages: [
            { role: "user", content: prompt }
          ]
        })
      });

      if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
      }

      const data = await response.json();
      
      let questionsText = '';
      if (data.content && Array.isArray(data.content)) {
        for (const item of data.content) {
          if (item.type === 'text' && item.text) {
            questionsText += item.text;
          }
        }
      } else if (typeof data === 'string') {
        questionsText = data;
      } else if (data.error) {
        throw new Error(data.error.message || 'API error');
      }

      // Parse questions from the response
      const questionLines = questionsText
        .split('\n')
        .filter(line => line.trim() && /^\d+\./.test(line.trim()))
        .map(line => line.replace(/^\d+\.\s*/, '').trim());

      if (questionLines.length >= 10) {
        setInterviewQuestions(questionLines.slice(0, 10));
      } else {
        // Fallback to generic questions if parsing fails
        const fallbackQuestions = interviewQuestionsFallback[difficulty][jobLevel];
        setInterviewQuestions(fallbackQuestions);
      }
    } catch (error) {
      console.error('Error generating questions:', error);
      // Fallback to role-specific questions
      const fallbackQuestions = interviewQuestionsFallback[difficulty][jobLevel];
      setInterviewQuestions(fallbackQuestions);
    } finally {
      setIsLoadingQuestions(false);
    }
  };

  const interviewQuestionsFallback = {
    easy: {
      entry: [
        "Walk me through your background and explain how your experience relates specifically to the core responsibilities of this role.",
        "What specific tools, software, or methodologies relevant to this position have you worked with, and how proficient are you with each?",
        "Describe a project or task where you demonstrated the key skills required for this job. What was your approach and what was the outcome?",
        "What do you understand about the typical day-to-day responsibilities in this role, and how have you prepared yourself to handle them?",
        "Tell me about a time when you had to learn something new that's relevant to this position. How did you approach the learning process?",
        "How do you stay current with trends, best practices, and developments in this field?",
        "Describe a situation where you collaborated with the types of stakeholders this role typically works with. How did you ensure effective communication?",
        "What attracts you specifically to this position, and how does it align with your career development goals?",
        "Tell me about a challenge you've faced that's similar to what someone in this role might encounter. How did you handle it?",
        "What questions do you have about the role, team, or organization that would help you understand if this is the right fit?"
      ],
      mid: [
        "Walk me through a comprehensive project where you demonstrated the core competencies and technical skills needed for this role.",
        "What specific methodologies, frameworks, or best practices do you regularly use that are directly relevant to this position?",
        "Describe in detail how you've handled the typical challenges and complex situations that come with this type of role.",
        "Tell me about your experience with the key technologies, tools, or processes that are central to this position's success.",
        "How have you mentored, trained, or influenced others in developing skills that are relevant to this role?",
        "Describe a time when you had to balance competing priorities or manage multiple projects simultaneously in a similar context.",
        "What's your approach to problem-solving when faced with the technical or strategic challenges specific to this role?",
        "Tell me about a situation where you had to collaborate cross-functionally with teams or departments this role typically works with.",
        "How do you measure success and track performance in the key areas this position is responsible for?",
        "What innovations or improvements have you implemented in areas related to this role's core functions?"
      ],
      senior: [
        "Describe how you've driven strategic initiatives at an organizational level that are similar to what this role requires.",
        "What's your comprehensive approach to managing teams, projects, and budgets at the scale and complexity this position demands?",
        "Tell me about the key leadership challenges specific to this role and how your experience has prepared you to address them.",
        "How do you develop and execute long-term strategy while managing day-to-day operations in the areas this position oversees?",
        "Describe your experience building, developing, and scaling high-performing teams in functions similar to this role.",
        "What's your approach to organizational change management, particularly when implementing initiatives this role would be responsible for?",
        "How do you balance stakeholder expectations, resource constraints, and strategic objectives in the context of this position?",
        "Tell me about your experience with the governance, compliance, or risk management aspects relevant to this role.",
        "Describe how you measure success and create accountability for the key outcomes this position is responsible for delivering.",
        "What's your vision for how someone in this role can create transformational impact on the organization?"
      ]
    },
    medium: {
      entry: [
        "Tell me about a time when you had to quickly learn and apply a new skill or tool that's essential for this role. Walk me through your learning process and how you applied it.",
        "Describe a typical challenge that someone in this position faces regularly. How would you approach solving it, and what experience do you have with similar situations?",
        "What relevant certifications, training programs, self-study, or hands-on projects have you completed to prepare yourself for this type of work?",
        "Walk me through a situation where you had to collaborate with multiple stakeholders or teams similar to what this role requires. How did you manage different perspectives and priorities?",
        "Describe a project where you used data, metrics, or analysis in ways that are relevant to the decision-making this role involves.",
        "Tell me about a time when you identified a problem or opportunity in an area related to this role. What actions did you take and what was the result?",
        "How do you prioritize your work when facing multiple deadlines and demands, particularly in the context of responsibilities this role handles?",
        "Describe a situation where you had to adapt your communication style or approach when working with different audiences that this role interacts with.",
        "Tell me about a time when you received critical feedback on work related to this role's responsibilities. How did you respond and what did you learn?",
        "What's your process for staying organized and ensuring quality when managing the types of deliverables this position is responsible for?"
      ],
      mid: [
        "Describe a complex, multi-faceted problem you solved that required the specific technical or functional skills central to this position. Walk me through your approach from problem identification to solution implementation.",
        "Tell me about a time when you had to balance competing priorities from different stakeholders while maintaining the quality and timeliness of deliverables this role produces.",
        "Walk me through your comprehensive experience with the specific tools, systems, and methodologies that this position relies on daily. Include examples of how you've used them to drive results.",
        "Describe a situation where you had to influence or persuade others without direct authority, in a context similar to what this role faces.",
        "Tell me about how you approach continuous improvement and optimization in the functional areas this role is responsible for. Provide specific examples of improvements you've implemented.",
        "Describe a time when you had to make a difficult decision with incomplete information in an area related to this role's responsibilities. What was your decision-making process?",
        "How have you handled situations where projects or initiatives didn't go as planned in contexts similar to this role? What did you learn?",
        "Tell me about your experience managing budgets, resources, or vendor relationships at the level this position requires.",
        "Describe how you've built credibility and trust with senior leaders or key stakeholders that this role needs to influence and partner with.",
        "Walk me through a situation where you identified a strategic opportunity or risk in an area this role oversees. How did you approach it and what was the outcome?"
      ],
      senior: [
        "Describe in detail how you've transformed operations, strategy, or organizational capabilities in ways that align with what this role requires. What was the scope, your approach, and the results?",
        "Tell me about your experience building, restructuring, or scaling teams and organizational functions at the complexity level this position demands.",
        "Walk me through the key strategic decisions this role needs to make regularly and how your experience has prepared you to make them effectively.",
        "Describe how you've managed significant budgets and allocated resources across competing priorities at the scale this position oversees.",
        "Tell me about driving major organizational change, particularly when facing resistance from stakeholders at levels this role must influence.",
        "How do you balance short-term operational demands with long-term strategic vision in the areas this position is responsible for?",
        "Describe your experience with organizational governance, risk management, and compliance in contexts similar to what this role oversees.",
        "Tell me about building and maintaining executive-level relationships and partnerships that are critical to this role's success.",
        "Walk me through how you've established metrics, accountability structures, and performance management systems for areas this position owns.",
        "What's your comprehensive vision for how this role can drive transformational impact, and how does your experience prepare you to execute on that vision?"
      ]
    },
    hard: {
      entry: [
        "If given a high-priority project tomorrow requiring the top 5 skills for this role, walk me through in detail how you'd approach it from start to finish, including how you'd handle any gaps in your current knowledge.",
        "Describe your comprehensive plan for the first 90 days in this position, including how you'd get up to speed on critical aspects, build relationships, and start delivering value.",
        "Tell me about a time when you had to deliver significant results despite having clear gaps in experience relevant to this role. How did you compensate and what was the outcome?",
        "You're three months into this role and a major stakeholder questions whether you have the right qualifications and experience. Walk me through how you'd handle this situation and rebuild confidence.",
        "Describe your detailed learning and development plan for quickly becoming proficient in the areas where you currently have less experience but this role requires expertise.",
        "Walk me through a complex scenario where you had to make a critical decision with limited guidance in an area related to this role's responsibilities. What was your process?",
        "Tell me about navigating a politically sensitive situation involving stakeholders at different levels while trying to achieve an objective similar to what this role faces.",
        "Describe how you would approach building credibility and trust quickly with a skeptical team or leader in the context of this role's responsibilities.",
        "If you inherited underperforming processes, tools, or systems in areas this role is responsible for, walk me through your comprehensive approach to turning things around.",
        "Tell me about a situation where you had to challenge conventional thinking or push back on senior leaders regarding decisions that impact areas this role oversees."
      ],
      mid: [
        "Walk me through in extensive detail how you'd handle a major crisis typical to this role, with multiple urgent stakeholder demands, competing priorities, and significant organizational impact. What's your hour-by-hour approach?",
        "Describe the most difficult decision you've had to make that's directly similar to the types of decisions this position faces regularly. What was the context, your decision-making framework, and how did you handle the aftermath?",
        "You're new to this role and need to quickly build credibility with skeptical teams, demanding executives, and external stakeholders. Walk me through your comprehensive strategy for the first six months.",
        "Tell me about a time when you had to navigate complex organizational politics while still delivering on core objectives similar to what this role is accountable for. How did you balance relationships and results?",
        "You inherit significantly underperforming systems, processes, or teams in areas this role owns, with limited budget and resistance to change. Describe your detailed turnaround strategy.",
        "Walk me through managing a situation where you have three critical projects with hard deadlines, insufficient resources, and conflicting stakeholder demands - all typical challenges for this role.",
        "Describe a time when you had to make an unpopular but necessary decision that negatively impacted people or teams, in a context similar to this role's responsibilities. How did you handle it?",
        "Tell me about driving a significant change initiative against strong resistance from influential stakeholders that this role must work with. What was your approach to gaining buy-in?",
        "You discover a major problem or risk that previous people in similar roles missed or ignored. Walk me through how you'd handle the situation, including communicating upward and managing fallout.",
        "Describe your approach to building and executing a comprehensive strategy when you have incomplete data, tight timelines, and high stakes - typical conditions for decisions this role makes."
      ],
      senior: [
        "Describe in comprehensive detail a situation where you transformed a significantly underperforming department or organization at a scale similar to what this position oversees. Include the political challenges, your strategy, and measurable outcomes.",
        "Walk me through a scenario where your strategic vision for areas this role is responsible for conflicts fundamentally with the board's or CEO's direction. How would you navigate this situation?",
        "Tell me about the highest-stakes decision you've made with incomplete information that had significant financial or organizational implications, in a context similar to what this role faces. What was your framework?",
        "You need to drive a controversial, transformational change across the organization that this role is responsible for leading, while facing active resistance from powerful stakeholders. Walk me through your comprehensive change management strategy.",
        "How do you balance short-term profitability demands with long-term sustainability, innovation, and strategic positioning in the areas this position oversees, particularly when these objectives conflict?",
        "Describe managing a major crisis or failure in an area this role is accountable for, including handling executive scrutiny, stakeholder communication, and organizational recovery.",
        "You inherit a leadership team in areas this role oversees that has significant performance issues and cultural problems. Walk me through your detailed approach to assessment, decisions, and transformation.",
        "Tell me about a time when you had to make a decision that would benefit the organization long-term but cause short-term pain or resistance, particularly in areas this role is responsible for.",
        "Describe your approach to resource allocation when you have far more strategic priorities than available budget or capacity, which is typical for the scope this position manages.",
        "Walk me through how you would establish executive presence, influence C-suite decisions, and drive strategic impact in the first year in this role, particularly if you're coming from outside the organization."
      ]
    }
  };

  const calculateScore = (answers, feedback, difficulty, sessionTime) => {
    let score = 0;

    // Answer quality scoring (70 points total)
    answers.forEach((answer) => {
      const wordCount = answer.trim().split(/\s+/).length;
      
      // Length scoring (14 points per answer)
      if (wordCount >= 100) score += 14;
      else if (wordCount >= 50) score += 10;
      else if (wordCount >= 30) score += 6;
      else score += 2;

      // Detail scoring (check for examples, specific details) - 3 points
      if (answer.toLowerCase().includes('example') || 
          answer.toLowerCase().includes('for instance') ||
          answer.toLowerCase().includes('specifically')) {
        score += 3;
      }

      // Personal experience (use of "I", "my", "we") - 3 points
      const personalWords = (answer.match(/\b(i|my|we|our)\b/gi) || []).length;
      if (personalWords >= 5) score += 3;
    });

    // Time efficiency bonus (10 points)
    const avgTimePerQuestion = sessionTime / answers.length;
    if (avgTimePerQuestion >= 60 && avgTimePerQuestion <= 180) {
      score += 10;
    } else if (avgTimePerQuestion >= 30 && avgTimePerQuestion <= 240) {
      score += 5;
    }

    // Difficulty multiplier
    const difficultyMultipliers = { easy: 0.85, medium: 1.0, hard: 1.15 };
    score = Math.min(100, Math.round(score * difficultyMultipliers[difficulty]));

    return score;
  };

  const getScoreCategory = (score) => {
    if (score >= 90) return { 
      text: 'Excellent!', 
      grade: 'A',
      color: 'text-green-600', 
      bg: 'bg-green-50', 
      border: 'border-green-200' 
    };
    if (score >= 70) return { 
      text: 'Almost There!', 
      grade: 'B',
      color: 'text-blue-600', 
      bg: 'bg-blue-50', 
      border: 'border-blue-200' 
    };
    if (score >= 50) return { 
      text: 'Get More Practice', 
      grade: 'C',
      color: 'text-yellow-600', 
      bg: 'bg-yellow-50', 
      border: 'border-yellow-200' 
    };
    return { 
      text: 'Fail - Needs Improvement', 
      grade: 'F',
      color: 'text-red-600', 
      bg: 'bg-red-50', 
      border: 'border-red-200' 
    };
  };

  const getHireChance = (score) => {
    if (score >= 90) return 90 + Math.floor(Math.random() * 10);
    if (score >= 70) return 75 + Math.floor(Math.random() * 15);
    if (score >= 50) return 45 + Math.floor(Math.random() * 25);
    return 15 + Math.floor(Math.random() * 25);
  };

  const generateGradeFeedback = async (score, answers, jobRole, jobLevel, difficulty) => {
    setIsLoadingFeedback(true);
    const grade = getScoreCategory(score).grade;
    
    try {
      let prompt = '';
      
      if (grade === 'F') {
        // Grade F: Search for job role requirements and provide detailed feedback
        prompt = `You are an expert career coach analyzing a failed interview performance (score: ${score}/100) for a ${jobRole} position at ${jobLevel} level.

The candidate's answers were:
${answers.map((ans, i) => `Question ${i + 1}: ${ans}`).join('\n\n')}

Provide comprehensive, actionable feedback:

1. ASSESSMENT: Brief honest assessment of why they failed (2-3 sentences)

2. WHAT HIRING MANAGERS LOOK FOR: List 5-7 specific skills, qualities, experiences, and talking points that ${jobRole} hiring managers expect to hear in interviews at the ${jobLevel} level. Be specific - mention actual tools, technologies, methodologies, frameworks, and types of projects.

3. WHAT WAS MISSING: Analyze what the candidate's answers lacked compared to what employers expect. What specific skills, experiences, or examples did they fail to mention?

4. WHAT THEY SHOULD HAVE SAID: Provide 4-5 concrete examples of strong talking points they could have used, such as:
   - Specific technologies/tools they should mention knowing
   - Types of projects or achievements they should reference
   - Metrics or outcomes they should quantify
   - Industry terminology or methodologies they should demonstrate knowledge of

5. ACTION PLAN: 3-4 concrete steps to improve (be specific about what to study, practice, or prepare)

Be encouraging but honest. Focus on exactly what they need to say in their next interview to succeed.`;

      } else if (grade === 'C') {
        prompt = `You are an expert career coach analyzing an interview that needs more practice (score: ${score}/100, Grade C) for a ${jobRole} position at ${jobLevel} level.

The candidate's answers were:
${answers.map((ans, i) => `Question ${i + 1}: ${ans}`).join('\n\n')}

Provide balanced, specific feedback:

1. STRENGTHS: What they did well - 2-3 specific strong points in their answers

2. GAPS & WEAKNESSES: What they did poorly - 2-3 specific areas where answers fell short of what ${jobRole} hiring managers expect

3. WHAT THEY MISSED: Key points, experiences, or skills they should have mentioned but didn't. What do ${jobRole} hiring managers specifically want to hear that was absent?

4. WHAT THEY SHOULD HAVE SAID INSTEAD: Provide 4-5 specific examples of stronger talking points:
   - Better ways to frame their experience
   - Specific tools, technologies, or methodologies to mention
   - Types of metrics or outcomes to emphasize
   - Industry-specific terminology to demonstrate expertise

5. HOW CLOSE TO PASSING: Explain specifically what improvements would get them to Grade B/A

6. NEXT STEPS: 3 actionable tips with concrete examples of what to prepare

Be constructive and specific about exactly what needs to change in their answers.`;

      } else if (grade === 'B') {
        prompt = `You are an expert career coach analyzing a strong interview (score: ${score}/100, Grade B) for a ${jobRole} position at ${jobLevel} level.

The candidate's answers were:
${answers.map((ans, i) => `Question ${i + 1}: ${ans}`).join('\n\n')}

Provide encouraging, refinement-focused feedback:

1. WHAT YOU DID EXCELLENTLY: 3-4 specific strengths - what aspects of their answers really impressed and why these matter to ${jobRole} hiring managers

2. WHAT ${jobRole} HIRING MANAGERS LOVED: Specific elements in their answers that align perfectly with what employers seek (mention specific skills, examples, or approaches they demonstrated well)

3. WHAT WOULD PUSH YOU TO GRADE A: Small refinements needed - be specific about what additional talking points, examples, or framing would make answers even stronger

4. MINOR POLISH SUGGESTIONS: 2-3 specific ways to elevate answers:
   - Additional metrics or outcomes to mention
   - Stronger ways to frame certain experiences
   - More impactful terminology or industry language
   - Better structure or storytelling techniques

5. FINAL ADVICE: You're interview-ready - here's how to maintain confidence and handle the final interview stages

Be highly positive but provide actionable ways to achieve perfection.`;

      } else if (grade === 'A') {
        prompt = `You are an expert career coach analyzing an excellent interview (score: ${score}/100, Grade A) for a ${jobRole} position at ${jobLevel} level.

The candidate's answers were:
${answers.map((ans, i) => `Question ${i + 1}: ${ans}`).join('\n\n')}

Provide validating, confidence-building feedback:

1. OUTSTANDING PERFORMANCE: 4-5 specific strengths - what made their answers exceptional and exactly what ${jobRole} hiring managers want to hear

2. WHY HIRING MANAGERS WILL LOVE YOU: Analyze specific elements of their answers that demonstrate they're the ideal candidate:
   - What skills, experiences, or knowledge they showcased perfectly
   - How their examples align with what ${jobRole} roles require
   - Effective techniques they used (storytelling, metrics, structure, etc.)

3. WHAT YOU DID THAT MOST CANDIDATES DON'T: Highlight what set them apart from typical candidates - what talking points, depth, or expertise they demonstrated that's rare

4. AREAS OF ABSOLUTE PERFECTION: Specific moments in their answers that were textbook excellent

5. TINY REFINEMENTS (IF ANY): Only if there are truly minor areas for improvement - be very specific

6. YOU'RE READY: Confidence-building message that they're fully prepared and should feel confident going into real interviews

Be highly validating and specific about why they succeeded.`;
      }

      const response = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 1000,
          messages: [
            { role: "user", content: prompt }
          ],
          ...(grade === 'F' ? {
            tools: [{
              type: "web_search_20250305",
              name: "web_search"
            }]
          } : {})
        })
      });

      if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
      }

      const data = await response.json();
      
      // Extract text from response
      let feedbackText = '';
      if (data.content && Array.isArray(data.content)) {
        for (const item of data.content) {
          if (item.type === 'text' && item.text) {
            feedbackText += item.text;
          }
        }
      } else if (typeof data === 'string') {
        feedbackText = data;
      } else if (data.error) {
        throw new Error(data.error.message || 'API error');
      }
      
      setGradeFeedback(feedbackText);
    } catch (error) {
      console.error('Error generating feedback:', error);
      setGradeFeedback('Unable to generate detailed feedback at this time. Please review your answers and try again.');
    } finally {
      setIsLoadingFeedback(false);
    }
  };

  const startInterview = async () => {
    if (!jobRole.trim()) {
      alert('Please enter a job role');
      return;
    }
    
    setIsLoadingQuestions(true);
    setCurrentScreen('loading');
    
    // Generate custom questions for the job role
    await generateInterviewQuestions(jobRole, jobLevel, difficulty);
    
    setCurrentScreen('interview');
    setCurrentQuestion(0);
    setAnswers([]);
    setFeedback([]);
    setSessionTime(0);
    setIsTimerRunning(true);
    
    // Save initial interview state
    await saveInterviewProgress(0, [], [], 0);
  };

  const saveInterviewProgress = async (questionIndex, answersArray, feedbackArray, time) => {
    if (!currentUser) return;
    
    const progressData = {
      jobRole,
      jobLevel,
      difficulty,
      currentQuestion: questionIndex,
      answers: answersArray,
      feedback: feedbackArray,
      sessionTime: time,
      lastUpdated: new Date().toISOString()
    };
    
    try {
      await window.storage.set(`interview-progress-${currentUser.email}`, JSON.stringify(progressData), false);
    } catch (error) {
      console.error('Failed to save progress:', error);
    }
  };

  const loadInterviewProgress = async (userEmail) => {
    const email = userEmail || currentUser?.email;
    if (!email) return null;
    
    try {
      const result = await window.storage.get(`interview-progress-${email}`, false);
      if (result && result.value) {
        return JSON.parse(result.value);
      }
    } catch (error) {
      console.log('No saved progress found:', error);
    }
    return null;
  };

  const clearInterviewProgress = async () => {
    if (!currentUser) return;
    
    try {
      await window.storage.delete(`interview-progress-${currentUser.email}`, false);
      setHasSavedProgress(false);
    } catch (error) {
      console.error('Failed to clear progress:', error);
    }
  };

  const resumeInterview = async () => {
    const progress = await loadInterviewProgress(currentUser?.email);
    if (progress) {
      setJobRole(progress.jobRole);
      setJobLevel(progress.jobLevel);
      setDifficulty(progress.difficulty);
      setCurrentQuestion(progress.currentQuestion);
      setAnswers(progress.answers);
      setFeedback(progress.feedback);
      setSessionTime(progress.sessionTime);
      setCurrentScreen('interview');
      setIsTimerRunning(true);
      setHasSavedProgress(false);
    }
  };

  const submitAnswer = async () => {
    if (!userAnswer.trim()) {
      alert('Please provide an answer');
      return;
    }

    const newAnswers = [...answers, userAnswer];
    setAnswers(newAnswers);
    
    // Get AI-powered feedback for this specific answer
    const answerFeedback = await getFeedback(userAnswer, currentQuestion, interviewQuestions[currentQuestion]);
    const newFeedback = [...feedback, answerFeedback];
    setFeedback(newFeedback);
    
    setUserAnswer('');

    if (currentQuestion < interviewQuestions.length - 1) {
      const nextQuestion = currentQuestion + 1;
      setCurrentQuestion(nextQuestion);
      
      // Save progress after each answer
      await saveInterviewProgress(nextQuestion, newAnswers, newFeedback, sessionTime);
    } else {
      setIsTimerRunning(false);
      const score = calculateScore(newAnswers, newFeedback, difficulty, sessionTime);
      setOverallScore(score);
      await saveToHistory(newAnswers, newFeedback, score);
      await clearInterviewProgress();
      setCurrentScreen('results');
      
      // Generate AI feedback based on grade
      await generateGradeFeedback(score, newAnswers, jobRole, jobLevel, difficulty);
    }
  };

  const saveToHistory = async (answers, feedback, score) => {
    const interview = {
      id: Date.now(),
      jobRole,
      jobLevel,
      difficulty,
      score,
      date: new Date().toLocaleDateString(),
      time: formatTime(sessionTime),
      answersCount: answers.length
    };

    const newHistory = [interview, ...interviewHistory].slice(0, 10); // Keep last 10
    setInterviewHistory(newHistory);
    
    try {
      await window.storage.set(`interview-history-${currentUser.email}`, JSON.stringify(newHistory), false);
    } catch (error) {
      console.error('Failed to save history:', error);
    }
  };

  const getFeedback = async (answer, questionIndex, question) => {
    setIsLoadingFeedback(true);
    
    try {
      const prompt = `You are an expert interview coach providing feedback for a candidate interviewing for a ${jobRole} position at the ${jobLevel} level.

Question Asked: "${question}"

Candidate's Answer: "${answer}"

Analyze this answer and provide specific, actionable feedback that helps them improve for their next interview. Focus on:

1. What specific elements hiring managers for ${jobRole} roles look for in answers to this type of question
2. What the candidate did well (be specific about strong points in their answer)
3. What the candidate missed or could improve (specific gaps or weaknesses)
4. What they SHOULD have said instead - provide 2-3 concrete examples of stronger talking points, specific skills, technologies, or experiences they could mention
5. How to structure a better answer using relevant frameworks (STAR method, etc.)

Format your response as:
✅ STRENGTHS: [What they did well]
⚠️ GAPS: [What's missing from their answer]
💡 WHAT TO SAY INSTEAD: [Specific examples of better talking points]
📋 IMPROVEMENT TIPS: [How to structure better answers]

Be specific to ${jobRole} - mention actual skills, tools, methodologies, and experiences relevant to this role.`;

      const response = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          model: "claude-sonnet-4-20250514",
          max_tokens: 800,
          messages: [
            { role: "user", content: prompt }
          ]
        })
      });

      if (!response.ok) {
        throw new Error(`API request failed: ${response.status}`);
      }

      const data = await response.json();
      
      let feedbackText = '';
      if (data.content && Array.isArray(data.content)) {
        for (const item of data.content) {
          if (item.type === 'text' && item.text) {
            feedbackText += item.text;
          }
        }
      }

      return [{
        type: 'detailed',
        text: feedbackText || 'Feedback generation in progress...'
      }];
      
    } catch (error) {
      console.error('Error generating feedback:', error);
      // Fallback to basic feedback
      const feedbackItems = [];
      const wordCount = answer.trim().split(/\s+/).length;
      
      if (wordCount < 50) {
        feedbackItems.push({
          type: 'warning',
          text: `⚠️ Your answer was brief (${wordCount} words). Hiring managers for ${jobRole} roles expect detailed, comprehensive answers. Aim for 150-250 words (2-3 minutes speaking).`
        });
        feedbackItems.push({
          type: 'tip',
          text: `💡 WHAT TO SAY INSTEAD: Include specific examples of tools, technologies, or methodologies you've used in ${jobRole} work. Mention measurable outcomes and your specific role in achieving them.`
        });
      } else if (wordCount >= 150) {
        feedbackItems.push({
          type: 'success',
          text: '✅ Excellent answer length - comprehensive and detailed.'
        });
      }

      const hasSTAR = (
        answer.toLowerCase().includes('situation') || 
        answer.toLowerCase().includes('task') || 
        answer.toLowerCase().includes('result')
      );
      
      if (!hasSTAR) {
        feedbackItems.push({
          type: 'warning',
          text: '⚠️ GAP: Your answer lacks clear structure. Hiring managers look for organized, story-driven responses.'
        });
        feedbackItems.push({
          type: 'tip',
          text: '💡 WHAT TO SAY INSTEAD: Structure your answer using STAR method - Situation (context), Task (your responsibility), Action (specific steps you took with tools/skills relevant to this role), Result (measurable outcomes).'
        });
      }

      const hasSpecifics = (
        answer.toLowerCase().includes('example') || 
        answer.toLowerCase().includes('specifically') ||
        answer.toLowerCase().includes('for instance') ||
        /\d+%|\d+ [a-z]+|increased|decreased|improved/i.test(answer)
      );

      if (!hasSpecifics) {
        feedbackItems.push({
          type: 'warning',
          text: `⚠️ GAP: Missing specific examples and metrics. ${jobRole} hiring managers want to hear concrete evidence of your skills.`
        });
        feedbackItems.push({
          type: 'tip',
          text: `💡 WHAT TO SAY INSTEAD: Mention specific tools, technologies, or frameworks used in ${jobRole} work. Include measurable results (e.g., "reduced processing time by 30%", "managed $500K budget", "led team of 8"). Name actual projects, clients, or initiatives.`
        });
      }

      return feedbackItems;
    } finally {
      setIsLoadingFeedback(false);
    }
  };

  // Voice interview functions
  const speakQuestion = (questionIndex) => {
    if (!interviewQuestions[questionIndex]) return;
    
    // Stop any current speech
    window.speechSynthesis.cancel();
    
    const utterance = new SpeechSynthesisUtterance(interviewQuestions[questionIndex]);
    utterance.rate = 0.95;
    utterance.pitch = 1.05;
    utterance.volume = 1;
    
    // Wait for voices to load and select British English voice
    const setVoice = () => {
      const voices = window.speechSynthesis.getVoices();
      // Prefer UK English voices (Google UK English, Microsoft voices, etc.)
      const ukVoice = voices.find(voice => 
        voice.lang === 'en-GB' || 
        voice.name.includes('UK') || 
        voice.name.includes('British') ||
        voice.name.includes('Daniel') ||
        voice.name.includes('Martha')
      );
      if (ukVoice) {
        utterance.voice = ukVoice;
      }
    };
    
    // Try to set voice immediately
    setVoice();
    
    // Also set voice when voices are loaded (in case they weren't ready)
    if (window.speechSynthesis.getVoices().length === 0) {
      window.speechSynthesis.onvoiceschanged = setVoice;
    }
    
    utterance.onstart = () => setIsSpeaking(true);
    utterance.onend = () => setIsSpeaking(false);
    utterance.onerror = () => setIsSpeaking(false);
    
    setCurrentUtterance(utterance);
    window.speechSynthesis.speak(utterance);
  };

  const stopSpeaking = () => {
    window.speechSynthesis.cancel();
    setIsSpeaking(false);
  };

  const startRecording = async () => {
    try {
      // Check if browser supports speech recognition
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      
      if (!SpeechRecognition) {
        const shouldType = confirm('Voice recording is not supported in your browser. Would you like to type your answer instead?');
        if (shouldType) {
          setInterviewMode('text');
        }
        return;
      }

      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.lang = 'en-US';
      
      let finalTranscript = '';
      let interimTranscript = '';
      
      recognition.onstart = () => {
        setIsRecording(true);
        finalTranscript = '';
        interimTranscript = '';
      };
      
      recognition.onresult = (event) => {
        interimTranscript = '';
        
        for (let i = event.resultIndex; i < event.results.length; i++) {
          const transcript = event.results[i][0].transcript;
          
          if (event.results[i].isFinal) {
            finalTranscript += transcript + ' ';
          } else {
            interimTranscript += transcript;
          }
        }
        
        // Update the answer field in real-time
        setUserAnswer(finalTranscript + interimTranscript);
      };
      
      recognition.onerror = (event) => {
        console.error('Speech recognition error:', event.error);
        setIsRecording(false);
        
        if (event.error === 'not-allowed') {
          alert('Microphone access denied. Please allow microphone access and try again.');
        } else if (event.error === 'no-speech') {
          alert('No speech detected. Please try again.');
        } else {
          alert('Error with speech recognition: ' + event.error + '. Please try again or switch to text mode.');
        }
      };
      
      recognition.onend = () => {
        setIsRecording(false);
        if (finalTranscript.trim()) {
          setUserAnswer(finalTranscript.trim());
        }
      };
      
      setMediaRecorder(recognition);
      recognition.start();
      
    } catch (error) {
      console.error('Error starting recording:', error);
      alert('Could not start recording: ' + error.message + '. Please try text mode instead.');
      setInterviewMode('text');
    }
  };

  const stopRecording = () => {
    if (mediaRecorder) {
      mediaRecorder.stop();
      setIsRecording(false);
    }
  };

  const restart = async () => {
    await clearInterviewProgress();
    setCurrentScreen('home');
    setJobRole('');
    setCurrentQuestion(0);
    setUserAnswer('');
    setAnswers([]);
    setFeedback([]);
    setSessionTime(0);
    setIsTimerRunning(false);
    setGradeFeedback(null);
    setIsLoadingFeedback(false);
  };

  const viewHistory = () => {
    setCurrentScreen('history');
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 p-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Briefcase className="w-8 h-8 text-indigo-600" />
              <div>
                <h1 className="text-3xl font-bold text-gray-800">AI Interview Chat (AIIC)</h1>
                <p className="text-gray-600 text-sm mt-1">Prepare for your next job interview with confidence</p>
              </div>
            </div>
            <div className="flex items-center gap-4">
              {isAuthenticated && currentUser && (
                <>
                  <div className="text-right">
                    <p className="text-sm font-semibold text-gray-800">{currentUser.name}</p>
                    <p className="text-xs text-gray-600">{currentUser.email}</p>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="px-4 py-2 bg-red-100 text-red-700 rounded-lg hover:bg-red-200 transition text-sm"
                  >
                    Logout
                  </button>
                </>
              )}
              {currentScreen !== 'history' && currentScreen !== 'login' && currentScreen !== 'signup' && interviewHistory.length > 0 && (
                <button
                  onClick={viewHistory}
                  className="flex items-center gap-2 px-4 py-2 bg-indigo-100 text-indigo-700 rounded-lg hover:bg-indigo-200 transition"
                >
                  <History className="w-4 h-4" />
                  History
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Login Screen */}
        {currentScreen === 'login' && (
          <div className="bg-white rounded-lg shadow-lg p-8 max-w-md mx-auto">
            <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">Login to Your Account</h2>
            
            {authError && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
                {authError}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Address
                </label>
                <input
                  type="email"
                  value={loginEmail}
                  onChange={(e) => setLoginEmail(e.target.value)}
                  placeholder="your.email@example.com"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  onKeyPress={(e) => e.key === 'Enter' && handleLogin()}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>
                <input
                  type="password"
                  value={loginPassword}
                  onChange={(e) => setLoginPassword(e.target.value)}
                  placeholder="Enter your password"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  onKeyPress={(e) => e.key === 'Enter' && handleLogin()}
                />
              </div>

              <button
                onClick={handleLogin}
                className="w-full bg-indigo-600 text-white py-3 rounded-lg font-semibold hover:bg-indigo-700 transition"
              >
                Login
              </button>

              <div className="text-center pt-4 border-t border-gray-200">
                <p className="text-sm text-gray-600">
                  Don't have an account?{' '}
                  <button
                    onClick={() => {
                      setCurrentScreen('signup');
                      setAuthError('');
                    }}
                    className="text-indigo-600 hover:text-indigo-700 font-semibold"
                  >
                    Sign up
                  </button>
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Signup Screen */}
        {currentScreen === 'signup' && (
          <div className="bg-white rounded-lg shadow-lg p-8 max-w-md mx-auto">
            <h2 className="text-2xl font-bold text-gray-800 mb-6 text-center">Create Your Account</h2>
            
            {authError && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
                {authError}
              </div>
            )}

            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Full Name
                </label>
                <input
                  type="text"
                  value={signupName}
                  onChange={(e) => setSignupName(e.target.value)}
                  placeholder="John Doe"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Email Address
                </label>
                <input
                  type="email"
                  value={signupEmail}
                  onChange={(e) => setSignupEmail(e.target.value)}
                  placeholder="your.email@example.com"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Password
                </label>
                <input
                  type="password"
                  value={signupPassword}
                  onChange={(e) => setSignupPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Confirm Password
                </label>
                <input
                  type="password"
                  value={signupConfirmPassword}
                  onChange={(e) => setSignupConfirmPassword(e.target.value)}
                  placeholder="Re-enter your password"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  onKeyPress={(e) => e.key === 'Enter' && handleSignup()}
                />
              </div>

              <button
                onClick={handleSignup}
                className="w-full bg-indigo-600 text-white py-3 rounded-lg font-semibold hover:bg-indigo-700 transition"
              >
                Sign Up
              </button>

              <div className="text-center pt-4 border-t border-gray-200">
                <p className="text-sm text-gray-600">
                  Already have an account?{' '}
                  <button
                    onClick={() => {
                      setCurrentScreen('login');
                      setAuthError('');
                    }}
                    className="text-indigo-600 hover:text-indigo-700 font-semibold"
                  >
                    Login
                  </button>
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Loading Screen */}
        {currentScreen === 'loading' && (
          <div className="bg-white rounded-lg shadow-lg p-8">
            <div className="text-center py-12">
              <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-indigo-600 mx-auto mb-6"></div>
              <h2 className="text-2xl font-bold text-gray-800 mb-2">Preparing Your Interview</h2>
              <p className="text-gray-600">Generating personalized questions for {jobRole}...</p>
            </div>
          </div>
        )}

        {/* Home Screen */}
        {currentScreen === 'home' && (
          <div className="bg-white rounded-lg shadow-lg p-8">
            {hasSavedProgress && (
              <div className="bg-green-50 border-2 border-green-200 rounded-lg p-6 mb-6">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-lg font-bold text-green-900 mb-2">Resume Your Interview</h3>
                    <p className="text-green-800 text-sm mb-4">
                      You have an interview in progress. Would you like to continue where you left off?
                    </p>
                  </div>
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={resumeInterview}
                    className="flex-1 bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition"
                  >
                    Resume Interview
                  </button>
                  <button
                    onClick={async () => {
                      await clearInterviewProgress();
                    }}
                    className="flex-1 bg-red-100 text-red-700 py-3 rounded-lg font-semibold hover:bg-red-200 transition"
                  >
                    Start Fresh
                  </button>
                </div>
              </div>
            )}

            <h2 className="text-2xl font-bold text-gray-800 mb-6">Start Your Practice Interview</h2>
            
            <div className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  What job role are you applying for?
                </label>
                <input
                  type="text"
                  value={jobRole}
                  onChange={(e) => setJobRole(e.target.value)}
                  placeholder="e.g., Software Engineer, Marketing Manager, Sales Associate"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Experience Level
                  </label>
                  <select
                    value={jobLevel}
                    onChange={(e) => setJobLevel(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  >
                    <option value="entry">Entry Level (0-2 years)</option>
                    <option value="mid">Mid Level (3-7 years)</option>
                    <option value="senior">Senior Level (8+ years)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Difficulty Level
                  </label>
                  <select
                    value={difficulty}
                    onChange={(e) => setDifficulty(e.target.value)}
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
                  >
                    <option value="easy">Easy - Basic Questions</option>
                    <option value="medium">Medium - Standard Interview</option>
                    <option value="hard">Hard - Challenging Scenarios</option>
                  </select>
                </div>
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                <h3 className="font-semibold text-blue-900 mb-2">Interview Tips:</h3>
                <ul className="space-y-1 text-sm text-blue-800">
                  <li>• You'll answer 10 in-depth questions (expect 2-3 minutes per answer)</li>
                  <li>• Use specific examples from your experience with the STAR method</li>
                  <li>• Be detailed about tools, processes, and methodologies you've used</li>
                  <li>• Questions will be tailored to the actual requirements of the role</li>
                  <li>• Demonstrate your hands-on experience with role-specific challenges</li>
                </ul>
              </div>

              <button
                onClick={startInterview}
                className="w-full bg-indigo-600 text-white py-4 rounded-lg font-semibold hover:bg-indigo-700 transition flex items-center justify-center gap-2"
              >
                <Play className="w-5 h-5" />
                Start Interview
              </button>
            </div>
          </div>
        )}

        {/* Interview Screen */}
        {currentScreen === 'interview' && (
          <div className="bg-white rounded-lg shadow-lg p-8">
            <div className="mb-6">
              <div className="flex justify-between items-center mb-4">
                <div className="flex items-center gap-4">
                  <span className="text-sm font-medium text-gray-600">
                    Question {currentQuestion + 1} of {interviewQuestions.length}
                  </span>
                  <div className="flex items-center gap-2 bg-indigo-100 px-3 py-1 rounded-full">
                    <Clock className="w-4 h-4 text-indigo-600" />
                    <span className="text-sm font-semibold text-indigo-700">{formatTime(sessionTime)}</span>
                  </div>
                </div>
                <span className="text-sm text-gray-600 capitalize">
                  {difficulty} • {jobRole}
                </span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className="bg-indigo-600 h-2 rounded-full transition-all"
                  style={{ width: `${((currentQuestion + 1) / interviewQuestions.length) * 100}%` }}
                />
              </div>
            </div>

            <div className="bg-indigo-50 border-l-4 border-indigo-600 p-6 mb-6 rounded">
              <div className="flex items-start gap-3">
                <Mic className="w-6 h-6 text-indigo-600 flex-shrink-0 mt-1" />
                <div>
                  <h3 className="font-semibold text-indigo-900 mb-2">Interviewer Question:</h3>
                  <p className="text-lg text-gray-800">{interviewQuestions[currentQuestion]}</p>
                </div>
              </div>
            </div>

            <div className="space-y-4">
              <label className="block text-sm font-medium text-gray-700">
                Your Answer:
              </label>
              <textarea
                value={userAnswer}
                onChange={(e) => setUserAnswer(e.target.value)}
                placeholder="Type your answer here... (Aim for 1-2 minutes worth of speaking, or about 150-300 words)"
                rows={8}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent"
              />

              <button
                onClick={submitAnswer}
                className="w-full bg-indigo-600 text-white py-3 rounded-lg font-semibold hover:bg-indigo-700 transition"
              >
                Submit Answer & Continue
              </button>
            </div>
          </div>
        )}

        {/* Results Screen */}
        {currentScreen === 'results' && (
          <div className="bg-white rounded-lg shadow-lg p-8">
            <div className="text-center mb-8">
              <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
              <h2 className="text-3xl font-bold text-gray-800 mb-2">Interview Complete!</h2>
              <p className="text-gray-600">Total Time: {formatTime(sessionTime)}</p>
            </div>

            {/* Score Card */}
            <div className="bg-gradient-to-br from-indigo-50 to-purple-50 border-2 border-indigo-200 rounded-xl p-6 mb-8">
              <div className="text-center mb-4">
                <h3 className="text-xl font-semibold text-gray-800 mb-2">Interview Performance</h3>
                <div className="flex items-center justify-center gap-6">
                  <div>
                    <div className="text-5xl font-bold text-indigo-600 mb-1">{overallScore}%</div>
                    <div className={`text-sm font-semibold ${getScoreCategory(overallScore).color}`}>
                      {getScoreCategory(overallScore).text}
                    </div>
                  </div>
                  <div className={`text-7xl font-bold ${getScoreCategory(overallScore).color}`}>
                    {getScoreCategory(overallScore).grade}
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mt-6">
                <div className="bg-white rounded-lg p-4 text-center">
                  <TrendingUp className="w-8 h-8 text-indigo-600 mx-auto mb-2" />
                  <div className="text-2xl font-bold text-gray-800">{getHireChance(overallScore)}%</div>
                  <div className="text-sm text-gray-600">Estimated Hire Chance</div>
                </div>
                <div className="bg-white rounded-lg p-4 text-center">
                  <Award className="w-8 h-8 text-purple-600 mx-auto mb-2" />
                  <div className="text-2xl font-bold text-gray-800 capitalize">{difficulty}</div>
                  <div className="text-sm text-gray-600">Difficulty Level</div>
                </div>
              </div>

              <div className="mt-4 text-center text-sm text-gray-600">
                {overallScore >= 90 && "Outstanding performance! You're well-prepared for this interview."}
                {overallScore >= 70 && overallScore < 90 && "Great job! You're almost there - just a few tweaks needed."}
                {overallScore >= 50 && overallScore < 70 && "Keep practicing! You're on the right track but need more preparation."}
                {overallScore < 50 && "Don't give up! Use this feedback to improve and try again."}
              </div>
            </div>

            {/* AI-Generated Grade Feedback */}
            <div className="bg-white border-2 border-indigo-200 rounded-xl p-6 mb-8">
              <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                <Award className="w-6 h-6 text-indigo-600" />
                Grade {getScoreCategory(overallScore).grade} - Personalized Feedback
              </h3>
              
              {isLoadingFeedback ? (
                <div className="text-center py-8">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto mb-4"></div>
                  <p className="text-gray-600">Analyzing your interview performance...</p>
                </div>
              ) : gradeFeedback ? (
                <div className="prose max-w-none">
                  <div className="text-gray-700 whitespace-pre-line leading-relaxed">
                    {gradeFeedback}
                  </div>
                </div>
              ) : (
                <p className="text-gray-600">Feedback will appear here after analysis.</p>
              )}
            </div>

            {/* Detailed Feedback */}
            <div className="space-y-6">
              <h3 className="text-xl font-bold text-gray-800">Question-by-Question Feedback</h3>
              {interviewQuestions.map((question, index) => (
                <div key={index} className="border border-gray-200 rounded-lg p-6 bg-white shadow-sm">
                  <h3 className="font-semibold text-gray-800 mb-3 text-lg">
                    Q{index + 1}: {question}
                  </h3>
                  
                  <div className="bg-gray-50 p-4 rounded mb-4 border border-gray-200">
                    <p className="text-sm font-semibold text-gray-700 mb-2">Your Answer:</p>
                    <p className="text-gray-700 leading-relaxed">{answers[index]}</p>
                    <p className="text-xs text-gray-500 mt-3">
                      Word count: {answers[index].trim().split(/\s+/).length} words
                    </p>
                  </div>

                  <div className="space-y-3">
                    <p className="text-sm font-bold text-indigo-700 mb-2">Expert Feedback:</p>
                    {feedback[index]?.map((item, i) => (
                      <div
                        key={i}
                        className={`text-sm p-4 rounded-lg border-l-4 ${
                          item.type === 'success'
                            ? 'bg-green-50 text-green-900 border-green-500'
                            : item.type === 'warning'
                            ? 'bg-yellow-50 text-yellow-900 border-yellow-500'
                            : item.type === 'detailed'
                            ? 'bg-blue-50 text-blue-900 border-blue-500'
                            : 'bg-indigo-50 text-indigo-900 border-indigo-500'
                        }`}
                      >
                        <div className="whitespace-pre-line leading-relaxed">{item.text}</div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-8 flex gap-4">
              <button
                onClick={restart}
                className="flex-1 bg-indigo-600 text-white py-3 rounded-lg font-semibold hover:bg-indigo-700 transition flex items-center justify-center gap-2"
              >
                <RotateCcw className="w-5 h-5" />
                Start New Interview
              </button>
              <button
                onClick={viewHistory}
                className="flex-1 bg-gray-600 text-white py-3 rounded-lg font-semibold hover:bg-gray-700 transition flex items-center justify-center gap-2"
              >
                <History className="w-5 h-5" />
                View History
              </button>
            </div>
          </div>
        )}

        {/* History Screen */}
        {currentScreen === 'history' && (
          <div className="bg-white rounded-lg shadow-lg p-8">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-gray-800">Interview History</h2>
              <button
                onClick={() => setCurrentScreen('home')}
                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
              >
                New Interview
              </button>
            </div>

            {interviewHistory.length === 0 ? (
              <div className="text-center py-12">
                <History className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-600">No interview history yet. Start your first practice interview!</p>
              </div>
            ) : (
              <div className="space-y-4">
                {interviewHistory.map((interview) => {
                  const scoreCategory = getScoreCategory(interview.score);
                  return (
                    <div key={interview.id} className={`border-2 ${scoreCategory.border} ${scoreCategory.bg} rounded-lg p-6`}>
                      <div className="flex justify-between items-start">
                        <div className="flex-1">
                          <h3 className="text-lg font-bold text-gray-800">{interview.jobRole}</h3>
                          <div className="flex flex-wrap gap-3 mt-2 text-sm text-gray-600">
                            <span className="capitalize">Level: {interview.jobLevel}</span>
                            <span>•</span>
                            <span className="capitalize">Difficulty: {interview.difficulty}</span>
                            <span>•</span>
                            <span>Time: {interview.time}</span>
                            <span>•</span>
                            <span>{interview.date}</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className={`text-3xl font-bold ${scoreCategory.color}`}>{interview.score}%</div>
                          <div className={`text-sm font-semibold ${scoreCategory.color}`}>{scoreCategory.text}</div>
                          <div className="text-xs text-gray-600 mt-1">
                            {getHireChance(interview.score)}% hire chance
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
