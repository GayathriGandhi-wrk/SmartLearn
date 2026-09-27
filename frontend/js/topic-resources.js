// Topic learning resources - curated reference links with YouTube / W3Schools / doc search fallbacks
(function () {
  const YT_SEARCH = "https://www.youtube.com/results?search_query=";
  const GOOGLE = "https://www.google.com/search?q=";
  const WIKI_SEARCH = "https://en.wikipedia.org/w/index.php?search=";

  const enc = (s) => encodeURIComponent(String(s || "").trim());

  function isSafeUrl(url) {
    return typeof url === "string" && /^https:\/\/[^/]+/i.test(url.trim());
  }

  function item(title, url, source, kind, meta) {
    if (!isSafeUrl(url)) return null;
    const entry = {
      title: String(title || "").trim(),
      url: url.trim(),
      source: String(source || "").trim(),
      kind: kind || "search",
    };
    if (meta) entry.meta = String(meta).trim();
    return entry.title ? entry : null;
  }

  function dedupe(list) {
    const seen = new Set();
    return list.filter(Boolean).filter((r) => {
      const key = r.url.toLowerCase().replace(/\/$/, "");
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }

  // Reference sites keyed by a keyword that must appear in the subject name.
  const SITES = [
    { id: "w3schools", name: "W3Schools", search: (q) => `${GOOGLE}${enc(q + " site:w3schools.com")}` },
    { id: "mdn", name: "MDN Web Docs", search: (q) => `https://developer.mozilla.org/en-US/search?q=${enc(q)}` },
    { id: "gfg", name: "GeeksforGeeks", search: (q) => `${GOOGLE}${enc(q + " site:geeksforgeeks.org")}` },
    { id: "javatpoint", name: "JavaTpoint", search: (q) => `${GOOGLE}${enc(q + " site:javatpoint.com")}` },
    { id: "tutorialspoint", name: "TutorialsPoint", search: (q) => `${GOOGLE}${enc(q + " site:tutorialspoint.com")}` },
    { id: "wikipedia", name: "Wikipedia", search: (q) => WIKI_SEARCH + enc(q) },
  ];

  // Technology hubs. `keys` match the subject name; the first entry whose key
  // matches becomes the primary reference for that subject.
  const TECHS = [
    { key: "html", label: "HTML", refTitle: "HTML Reference (MDN)", refUrl: "https://developer.mozilla.org/en-US/docs/Web/HTML", learnUrl: "https://developer.mozilla.org/en-US/docs/Learn_web_development/Core/Structuring_content", sites: ["w3schools", "mdn"] },
    { key: "css", label: "CSS", refTitle: "CSS Reference (MDN)", refUrl: "https://developer.mozilla.org/en-US/docs/Web/CSS", learnUrl: "https://web.dev/learn/css", sites: ["w3schools", "mdn"] },
    { key: "javascript", label: "JavaScript", refTitle: "JavaScript Reference (MDN)", refUrl: "https://developer.mozilla.org/en-US/docs/Web/JavaScript", learnUrl: "https://javascript.info/", sites: ["w3schools", "mdn"] },
    { key: "typescript", label: "TypeScript", refTitle: "TypeScript Handbook", refUrl: "https://www.typescriptlang.org/docs/handbook/intro.html", learnUrl: "https://www.typescriptlang.org/playground", sites: ["gfg", "wikipedia"] },
    { key: "react", label: "React", refTitle: "React Learn", refUrl: "https://react.dev/learn", learnUrl: "https://react.dev/learn/tutorial-tic-tac-toe", sites: ["w3schools", "gfg"] },
    { key: "angular", label: "Angular", refTitle: "Angular Docs", refUrl: "https://angular.dev/", learnUrl: "https://angular.dev/tutorials", sites: ["gfg", "wikipedia"] },
    { key: "vue", label: "Vue", refTitle: "Vue Docs", refUrl: "https://vuejs.org/guide/introduction.html", learnUrl: "https://vuejs.org/tutorial/", sites: ["gfg", "wikipedia"] },
    { key: "node", label: "Node.js", refTitle: "Node.js API Docs", refUrl: "https://nodejs.org/docs/latest/api/", learnUrl: "https://nodejs.org/en/learn", sites: ["w3schools", "mdn"] },
    { key: "express", label: "Express", refTitle: "Express Guide", refUrl: "https://expressjs.com/en/guide/routing.html", learnUrl: "https://expressjs.com/en/starter/installing.html", sites: ["gfg", "wikipedia"] },
    { key: "django", label: "Django", refTitle: "Django Docs", refUrl: "https://docs.djangoproject.com/en/stable/", learnUrl: "https://docs.djangoproject.com/en/stable/intro/tutorial01/", sites: ["gfg", "wikipedia"] },
    { key: "flask", label: "Flask", refTitle: "Flask Docs", refUrl: "https://flask.palletsprojects.com/en/stable/", learnUrl: "https://flask.palletsprojects.com/en/stable/quickstart/", sites: ["gfg", "wikipedia"] },
    { key: "php", label: "PHP", refTitle: "PHP Manual", refUrl: "https://www.php.net/manual/en/", learnUrl: "https://www.php.net/manual/en/tutorial.php", sites: ["w3schools", "wikipedia"] },
    { key: "java", label: "Java", refTitle: "Java Documentation (Oracle)", refUrl: "https://docs.oracle.com/en/java/", learnUrl: "https://dev.java/learn/", sites: ["w3schools", "javatpoint"] },
    { key: "kotlin", label: "Kotlin", refTitle: "Kotlin Docs", refUrl: "https://kotlinlang.org/docs/home.html", learnUrl: "https://kotlinlang.org/docs/getting-started.html", sites: ["gfg", "wikipedia"] },
    { key: "swift", label: "Swift", refTitle: "Swift.org", refUrl: "https://www.swift.org/documentation/", learnUrl: "https://www.swift.org/documentation/swift-book/", sites: ["gfg", "wikipedia"] },
    { key: "python", label: "Python", refTitle: "Python Official Docs", refUrl: "https://docs.python.org/3/", learnUrl: "https://docs.python.org/3/tutorial/", sites: ["w3schools", "gfg"] },
    { key: "pandas", label: "pandas", refTitle: "pandas Docs", refUrl: "https://pandas.pydata.org/docs/", learnUrl: "https://pandas.pydata.org/docs/getting_started/index.html", sites: ["gfg", "wikipedia"] },
    { key: "numpy", label: "NumPy", refTitle: "NumPy Docs", refUrl: "https://numpy.org/doc/stable/", learnUrl: "https://numpy.org/doc/stable/user/quickstart.html", sites: ["gfg", "wikipedia"] },
    { key: "r", label: "R", refTitle: "R Documentation", refUrl: "https://www.r-project.org/documentation.html", learnUrl: "https://cran.r-project.org/web/packages/R-tutorial/R-tutorial.html", sites: ["wikipedia"] },
    { key: "c", label: "C", refTitle: "cppreference - C", refUrl: "https://en.cppreference.com/w/c", learnUrl: "https://en.cppreference.com/w/c/language", sites: ["gfg", "tutorialspoint"] },
    { key: "c++", alt: ["c++", "cpp"], label: "C++", refTitle: "cppreference - C++", refUrl: "https://en.cppreference.com/w/cpp", learnUrl: "https://isocpp.org/get-started", sites: ["gfg", "tutorialspoint"] },
    { key: "c#", alt: ["c#", "csharp", "c sharp"], label: "C#", refTitle: "Microsoft C# Guide", refUrl: "https://learn.microsoft.com/en-us/dotnet/csharp/", learnUrl: "https://learn.microsoft.com/en-us/dotnet/csharp/tour-of-csharp/", sites: ["gfg", "wikipedia"] },
    { key: "assembly", label: "Assembly", refTitle: "NASM Documentation", refUrl: "https://www.nasm.us/doc/", learnUrl: "https://www.nasm.us/doc/nasmdox.html", sites: ["gfg", "wikipedia"] },
    { key: "sql", label: "SQL", refTitle: "SQL Tutorial (W3Schools)", refUrl: "https://www.w3schools.com/sql/", learnUrl: "https://www.w3schools.com/sql/sql_intro.asp", sites: ["w3schools", "gfg"] },
    { key: "mysql", label: "MySQL", refTitle: "MySQL Reference", refUrl: "https://dev.mysql.com/doc/refman/8.4/en/", learnUrl: "https://dev.mysql.com/doc/", sites: ["w3schools", "gfg"] },
    { key: "postgresql", label: "PostgreSQL", refTitle: "PostgreSQL Docs", refUrl: "https://www.postgresql.org/docs/current/", learnUrl: "https://www.postgresql.org/docs/current/tutorial.html", sites: ["gfg", "wikipedia"] },
    { key: "mongodb", label: "MongoDB", refTitle: "MongoDB Manual", refUrl: "https://www.mongodb.com/docs/manual/", learnUrl: "https://www.mongodb.com/docs/manual/tutorial/", sites: ["gfg", "wikipedia"] },
    { key: "redis", label: "Redis", refTitle: "Redis Docs", refUrl: "https://redis.io/docs/latest/", learnUrl: "https://redis.io/docs/latest/get-started/", sites: ["gfg", "wikipedia"] },
    { key: "linux", label: "Linux", refTitle: "Linux Man Pages", refUrl: "https://man7.org/linux/man-pages/", learnUrl: "https://linuxjourney.com/", sites: ["gfg", "tutorialspoint"] },
    { key: "bash", label: "Bash / Shell", refTitle: "GNU Bash Manual", refUrl: "https://www.gnu.org/software/bash/manual/bash.html", learnUrl: "https://mywiki.wooledge.org/BashGuide", sites: ["gfg", "wikipedia"] },
    { key: "git", label: "Git", refTitle: "Pro Git (free book)", refUrl: "https://git-scm.com/book/en/v2", learnUrl: "https://git-scm.com/docs", sites: ["gfg", "wikipedia"] },
    { key: "docker", label: "Docker", refTitle: "Docker Docs", refUrl: "https://docs.docker.com/", learnUrl: "https://docs.docker.com/get-started/", sites: ["gfg", "wikipedia"] },
    { key: "kubernetes", label: "Kubernetes", refTitle: "Kubernetes Docs", refUrl: "https://kubernetes.io/docs/home/", learnUrl: "https://kubernetes.io/docs/tutorials/kubernetes-basics/", sites: ["gfg", "wikipedia"] },
    { key: "devops", label: "DevOps", refTitle: "DevOps Roadmap", refUrl: "https://roadmap.sh/devops", learnUrl: "https://github.com/devops-roadmap", sites: ["gfg", "wikipedia"] },
    { key: "aws", label: "AWS", refTitle: "AWS Documentation", refUrl: "https://docs.aws.amazon.com/", learnUrl: "https://aws.amazon.com/training/", sites: ["gfg", "wikipedia"] },
    { key: "azure", label: "Azure", refTitle: "Azure Docs", refUrl: "https://learn.microsoft.com/en-us/azure/", learnUrl: "https://learn.microsoft.com/en-us/azure/fundamentals/", sites: ["gfg", "wikipedia"] },
    { key: "gcp", label: "Google Cloud", refTitle: "Google Cloud Docs", refUrl: "https://cloud.google.com/docs", learnUrl: "https://cloud.google.com/docs/get-started", sites: ["gfg", "wikipedia"] },
    { key: "cybersecurity", label: "Cybersecurity", refTitle: "OWASP Cheat Sheet Series", refUrl: "https://cheatsheetseries.owasp.org/", learnUrl: "https://owasp.org/www-project-top-ten/", sites: ["gfg", "wikipedia"] },
    { key: "machine learning", label: "Machine Learning", refTitle: "scikit-learn User Guide", refUrl: "https://scikit-learn.org/stable/user_guide.html", learnUrl: "https://www.kaggle.com/learn/machine-learning", sites: ["gfg", "wikipedia"] },
    { key: "deep learning", label: "Deep Learning", refTitle: "TensorFlow Guide", refUrl: "https://www.tensorflow.org/guide", learnUrl: "https://www.tensorflow.org/tutorials", sites: ["gfg", "wikipedia"] },
    { key: "tensorflow", label: "TensorFlow", refTitle: "TensorFlow Guide", refUrl: "https://www.tensorflow.org/guide", learnUrl: "https://www.tensorflow.org/tutorials", sites: ["gfg", "wikipedia"] },
    { key: "pytorch", label: "PyTorch", refTitle: "PyTorch Docs", refUrl: "https://pytorch.org/docs/stable/index.html", learnUrl: "https://pytorch.org/tutorials/", sites: ["gfg", "wikipedia"] },
    { key: "data science", label: "Data Science", refTitle: "Kaggle Learn", refUrl: "https://www.kaggle.com/learn", learnUrl: "https://www.kaggle.com/learn/data-science", sites: ["gfg", "wikipedia"] },
    { key: "graphics", label: "Computer Graphics", refTitle: "LearnOpenGL", refUrl: "https://learnopengl.com/", learnUrl: "https://learnopengl.com/Getting_Started", sites: ["gfg", "wikipedia"] },
    { key: "flutter", label: "Flutter", refTitle: "Flutter Docs", refUrl: "https://docs.flutter.dev/", learnUrl: "https://docs.flutter.dev/get-started/install", sites: ["gfg", "wikipedia"] },
    { key: "android", label: "Android", refTitle: "Android Developers", refUrl: "https://developer.android.com/guide", learnUrl: "https://developer.android.com/training", sites: ["gfg", "wikipedia"] },
    { key: "blockchain", label: "Blockchain", refTitle: "Ethereum Docs", refUrl: "https://ethereum.org/en/developers/docs/", learnUrl: "https://ethereum.org/en/developers/tutorials/", sites: ["gfg", "wikipedia"] },
    { key: "mathematics", label: "Mathematics", refTitle: "Khan Academy Math", refUrl: "https://www.khanacademy.org/math", learnUrl: "https://www.khanacademy.org/math", sites: ["wikipedia"] },
    { key: "physics", label: "Physics", refTitle: "Khan Academy Physics", refUrl: "https://www.khanacademy.org/physics", learnUrl: "https://www.khanacademy.org/physics", sites: ["wikipedia"] },
    { key: "electronics", label: "Electronics", refTitle: "All About Circuits", refUrl: "https://www.allaboutcircuits.com/", learnUrl: "https://www.allaboutcircuits.com/textbook/", sites: ["wikipedia"] },
    { key: "biology", label: "Biology", refTitle: "Khan Academy Biology", refUrl: "https://www.khanacademy.org/science/biology", learnUrl: "https://www.khanacademy.org/science/biology", sites: ["wikipedia"] },
    { key: "genetics", label: "Genetics", refTitle: "Khan Academy Genetics", refUrl: "https://www.khanacademy.org/science/biology/genetics", learnUrl: "https://www.khanacademy.org/science/biology/genetics", sites: ["wikipedia"] },
    { key: "ecology", label: "Ecology", refTitle: "Khan Academy Ecology", refUrl: "https://www.khanacademy.org/science/biology/ecology", learnUrl: "https://www.khanacademy.org/science/biology/ecology", sites: ["wikipedia"] },
    { key: "accounting", label: "Accounting", refTitle: "AccountingCoach", refUrl: "https://www.accountingcoach.com/", learnUrl: "https://www.accountingcoach.com/accounting-principles", sites: ["gfg", "wikipedia"] },
    { key: "marketing", label: "Marketing", refTitle: "Marketing Examples", refUrl: "https://marketingexamples.com/", learnUrl: "https://marketingexamples.com/", sites: ["wikipedia"] },
    { key: "english", label: "Technical English", refTitle: "Khan Academy Grammar", refUrl: "https://www.khanacademy.org/humanities/grammar", learnUrl: "https://www.khanacademy.org/humanities/grammar", sites: ["wikipedia"] },
    { key: "communication", label: "Communication Skills", refTitle: "Toastmasters Public Speaking Guide", refUrl: "https://www.toastmasters.org/resources/public-speaking-guide", learnUrl: "https://www.toastmasters.org/resources/public-speaking-guide", sites: ["wikipedia"] },
    { key: "operating system", label: "Operating Systems", refTitle: "OSTEP (free textbook)", refUrl: "https://pages.cs.wisc.edu/~remzi/OSTEP/", learnUrl: "https://pages.cs.wisc.edu/~remzi/OSTEP/", sites: ["gfg", "tutorialspoint"] },
    { key: "networking", label: "Networking", refTitle: "Computer Networking Topical Guide", refUrl: "https://www.rfc-editor.org/", learnUrl: "https://www.rfc-editor.org/", sites: ["gfg", "tutorialspoint"] },
    { key: "database", label: "Databases", refTitle: "CMU Database Concepts", refUrl: "https://15445.courses.cs.cmu.edu/", learnUrl: "https://www.postgresql.org/docs/current/tutorial.html", sites: ["gfg", "tutorialspoint"] },
    { key: "algorithm", label: "Algorithms", refTitle: "VisuAlgo", refUrl: "https://visualgo.net/en", learnUrl: "https://visualgo.net/en", sites: ["gfg", "wikipedia"] },
    { key: "data structure", label: "Data Structures", refTitle: "VisuAlgo - data structures", refUrl: "https://visualgo.net/en", learnUrl: "https://visualgo.net/en", sites: ["gfg", "wikipedia"] },
    { key: "cloud computing", label: "Cloud", refTitle: "AWS Well-Architected Framework", refUrl: "https://aws.amazon.com/architecture/well-architected/", learnUrl: "https://aws.amazon.com/training/", sites: ["gfg", "wikipedia"] },
    { key: "artificial intelligence", label: "AI", refTitle: "Google Machine Learning Crash Course", refUrl: "https://developers.google.com/machine-learning/crash-course", learnUrl: "https://www.kaggle.com/learn/intro-to-machine-learning", sites: ["gfg", "wikipedia"] },
    { key: "nlp", label: "NLP", refTitle: "Hugging Face NLP Course", refUrl: "https://huggingface.co/learn/nlp-course", learnUrl: "https://www.kaggle.com/learn/natural-language-processing", sites: ["gfg", "wikipedia"] },
    { key: "computer vision", label: "Computer Vision", refTitle: "OpenCV Python Tutorials", refUrl: "https://docs.opencv.org/4.x/d6/d00/tutorial_py_root.html", learnUrl: "https://www.kaggle.com/learn/computer-vision", sites: ["gfg", "wikipedia"] },
    { key: "big data", label: "Big Data", refTitle: "Apache Spark Docs", refUrl: "https://spark.apache.org/docs/latest/", learnUrl: "https://spark.apache.org/examples.html", sites: ["gfg", "wikipedia"] },
    { key: "analytics", label: "Analytics", refTitle: "Kaggle Learn - Data Analysis", refUrl: "https://www.kaggle.com/learn/data-analysis", learnUrl: "https://www.kaggle.com/learn/data-analysis", sites: ["gfg", "wikipedia"] },
    { key: "time series", label: "Time Series", refTitle: "Kaggle Learn - Time Series", refUrl: "https://www.kaggle.com/learn/time-series", learnUrl: "https://www.kaggle.com/learn/time-series", sites: ["gfg", "wikipedia"] },
    { key: "software engineering", label: "Software Engineering", refTitle: "Software Engineering Roadmap", refUrl: "https://roadmap.sh/software-engineering", learnUrl: "https://github.com/softwareengineeringtopics/software-engineering-topics", sites: ["gfg", "wikipedia"] },
    { key: "computer architecture", label: "Computer Architecture", refTitle: "RISC-V ISA Specifications", refUrl: "https://riscv.org/technical/specifications/", learnUrl: "https://www.riscv.org/technical/specifications/", sites: ["gfg", "wikipedia"] },
    { key: "compiler design", label: "Compiler Design", refTitle: "Crafting Interpreters", refUrl: "https://craftinginterpreters.com/", learnUrl: "https://github.com/munificent/craftinginterpreters", sites: ["gfg", "wikipedia"] },
    { key: "human anatomy", label: "Human Anatomy", refTitle: "Khan Academy - Human anatomy", refUrl: "https://www.khanacademy.org/science/biology/human-anatomy", learnUrl: "https://www.khanacademy.org/science/biology/human-anatomy", sites: ["wikipedia"] },
    { key: "microbiology", label: "Microbiology", refTitle: "Khan Academy - Microbiology", refUrl: "https://www.khanacademy.org/science/biology/microbes", learnUrl: "https://www.khanacademy.org/science/biology/microbes", sites: ["wikipedia"] },
    { key: "business", label: "Business", refTitle: "AccountingCoach", refUrl: "https://www.accountingcoach.com/", learnUrl: "https://www.accountingcoach.com/business-accounting", sites: ["gfg", "wikipedia"] },
    { key: "web", label: "Web", refTitle: "MDN Web Docs", refUrl: "https://developer.mozilla.org/en-US/docs/Web", learnUrl: "https://developer.mozilla.org/en-US/docs/Learn_web_development", sites: ["w3schools", "mdn"] },
    { key: "cloud", label: "Cloud", refTitle: "AWS Well-Architected Framework", refUrl: "https://aws.amazon.com/architecture/well-architected/", learnUrl: "https://aws.amazon.com/training/", sites: ["gfg", "wikipedia"] },
    { key: "programming", label: "Programming", refTitle: "Programiz Tutorials", refUrl: "https://www.programiz.com/", learnUrl: "https://www.programiz.com/tutorials", sites: ["gfg", "wikipedia"] },
    { key: "backend", label: "Backend", refTitle: "MDN - Server-side scripting", refUrl: "https://developer.mozilla.org/en-US/docs/Server-side", learnUrl: "https://developer.mozilla.org/en-US/docs/Server-side", sites: ["gfg", "wikipedia"] },
    { key: "mobile", label: "Mobile", refTitle: "MDN - Progressive web apps", refUrl: "https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps", learnUrl: "https://web.dev/learn/pwa", sites: ["gfg", "wikipedia"] },
    { key: "distributed", label: "Distributed Systems", refTitle: "Designing Data-Intensive Applications (free online)", refUrl: "https://dataintensive.net/", learnUrl: "https://dataintensive.net/", sites: ["gfg", "wikipedia"] },
    { key: "file system", label: "File Systems", refTitle: "Linux Kernel Filesystems docs", refUrl: "https://www.kernel.org/doc/html/latest/filesystems/", learnUrl: "https://www.kernel.org/doc/html/latest/filesystems/", sites: ["gfg", "tutorialspoint"] },
    { key: "system programming", label: "System Programming", refTitle: "Linux man pages", refUrl: "https://man7.org/linux/man-pages/", learnUrl: "https://man7.org/linux/man-pages/", sites: ["gfg", "tutorialspoint"] },
    { key: "data mining", label: "Data Mining", refTitle: "scikit-learn User Guide", refUrl: "https://scikit-learn.org/stable/user_guide.html", learnUrl: "https://scikit-learn.org/stable/user_guide.html", sites: ["gfg", "wikipedia"] },
    { key: "forensic", label: "Forensics", refTitle: "Digital Forensics Magazine", refUrl: "https://www.digitalforensicsmagazine.com/", learnUrl: "https://www.digitalforensicsmagazine.com/", sites: ["gfg", "wikipedia"] },
    { key: "infrastructure", label: "Infrastructure", refTitle: "DevOps Roadmap", refUrl: "https://roadmap.sh/devops", learnUrl: "https://roadmap.sh/devops", sites: ["gfg", "wikipedia"] },
    { key: "banking", label: "Banking", refTitle: "Investopedia", refUrl: "https://www.investopedia.com/", learnUrl: "https://www.investopedia.com/terms/b/banking.asp", sites: ["wikipedia"] },
    { key: "taxation", label: "Taxation", refTitle: "Investopedia - Tax", refUrl: "https://www.investopedia.com/terms/t/tax.asp", learnUrl: "https://www.investopedia.com/terms/t/tax.asp", sites: ["wikipedia"] },
    { key: "dbms", label: "DBMS", refTitle: "CMU Database Concepts", refUrl: "https://15445.courses.cs.cmu.edu/", learnUrl: "https://www.postgresql.org/docs/current/tutorial.html", sites: ["gfg", "tutorialspoint"] },
    { key: "network", label: "Networking", refTitle: "IETF RFC Editor", refUrl: "https://www.rfc-editor.org/", learnUrl: "https://www.rfc-editor.org/", sites: ["gfg", "tutorialspoint"] },
    { key: "routing", label: "Routing", refTitle: "IETF Routing RFCs", refUrl: "https://www.rfc-editor.org/info/rfc1812", learnUrl: "https://www.rfc-editor.org/", sites: ["gfg", "tutorialspoint"] },
    { key: "emerging", label: "Emerging Technology", refTitle: "Developer Roadmaps", refUrl: "https://roadmap.sh/", learnUrl: "https://roadmap.sh/", sites: ["gfg", "wikipedia"] },
    { key: "discrete", label: "Discrete Mathematics", refTitle: "MIT OCW - Mathematics for Computer Science", refUrl: "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/", learnUrl: "https://ocw.mit.edu/courses/6-042j-mathematics-for-computer-science-fall-2010/", sites: ["gfg", "wikipedia"] },
  ];

  // Reliable channels for lecture-style videos, grouped by subject family.
  const CHANNELS = {
    theory: ["Neso Academy", "Gate Smashers", "MIT OpenCourseWare", "NPTEL", "Crash Course"],
    web: ["The Net Ninja", "Traversy Media", "freeCodeCamp.org", "GreatStack", "CodeWithHarry"],
    code: ["Programiz", "GreatStack", "freeCodeCamp.org", "Simplilearn", "CodeWithHarry"],
    data: ["StatQuest", "3Blue1Brown", "freeCodeCamp.org", "Simplilearn", "Khan Academy"],
    cloud: ["TechWorld with Nana", "Simplilearn", "freeCodeCamp.org", "Neso Academy", "GreatStack"],
    science: ["Khan Academy", "Crash Course", "NPTEL", "FreeCodeCamp", "Learn with Sumit"],
    business: ["AccountingCoach", "Khan Academy", "Crash Course", "Simplilearn", "Learn with Sumit"],
  };

  const FAMILIES = [
    { id: "web", keys: ["html", "css", "javascript", "react", "angular", "vue", "node", "express", "web", "frontend", "backend", "api", "mobile", "android", "flutter", "php", "typescript", "graphql", "microservice"] },
    { id: "data", keys: ["machine learning", "deep learning", "data science", "artificial intelligence", "ai", "nlp", "computer vision", "big data", "analytics", "statistics", "tensorflow", "pytorch"] },
    { id: "cloud", keys: ["cloud", "devops", "kubernetes", "docker", "aws", "azure", "gcp", "security", "cyber", "forensic", "blockchain", "infrastructure", "automation", "ci/cd"] },
    { id: "science", keys: ["biology", "biochemistry", "anatomy", "physiology", "genetics", "ecology", "microbiology", "biotechnology", "physics", "electronics", "chemistry", "environmental"] },
    { id: "business", keys: ["accounting", "commerce", "business", "economics", "management", "marketing", "banking", "taxation", "law", "cost", "budgeting", "finance", "sales", "english", "communication", "ethics"] },
    { id: "code", keys: ["programming", "c++", "cpp", "python", "java", "c", "c#", "kotlin", "swift", "algorithm", "data structure", "software", "compiler", "testing", "database", "sql", "mysql", "postgresql", "mongodb", "operating system", "linux", "network", "distributed", "graphics", "architecture", "mathematics", "emerging", "computing", "capstone", "project"] },
  ];

  function detectFamily(text) {
    const tokens = tokenize(text);
    let best = null;
    FAMILIES.forEach((f) => {
      const hits = f.keys.filter((k) => matchesKey(tokens, k)).length;
      if (hits && (!best || hits > best.hits)) best = { id: f.id, hits };
    });
    return best ? best.id : "theory";
  }

  function pickChannels(subjectText, topicName) {
    return CHANNELS[detectFamily(`${subjectText} ${topicName}`)] || CHANNELS.theory;
  }

  const byId = (id) => SITES.find((s) => s.id === id);

  function tokenize(value) {
    return " " + String(value || "").toLowerCase().replace(/[^a-z0-9+#]+/g, " ").trim().replace(/\s+/g, " ") + " ";
  }

  function matchesKey(tokens, key) {
    if (tokens.includes(" " + key + " ")) return true;
    if (key.length <= 2) return false;
    const words = key.split(" ");
    words[words.length - 1] += "s";
    return tokens.includes(" " + words.join(" ") + " ");
  }

  function detectTech(subjectText) {
    const raw = String(subjectText || "").toLowerCase();
    const tokens = tokenize(subjectText);
    return TECHS.filter((t) => {
      if (matchesKey(tokens, tokenize(t.key).trim())) return true;
      return (t.alt || []).some((p) => raw.includes(p));
    }).sort((a, b) => b.key.length - a.key.length);
  }

  function difficultyAngle(difficulty, estimatedHours) {
    const level = String(difficulty || "").toUpperCase();
    if (level === "BEGINNER") return { style: "for beginners", len: "introduction" };
    if (level === "ADVANCED") return { style: "in depth advanced", len: "full course" };
    if (Number(estimatedHours) >= 4) return { style: "in depth", len: "full course" };
    return { style: "explained simply", len: "tutorial" };
  }

  function buildVideos(subjectText, topicName, difficulty, hours) {
    const subject = String(subjectText || "").trim();
    const topic = String(topicName || "").trim();
    const angle = difficultyAngle(difficulty, hours);
    const base = subject ? `${topic} in ${subject}` : topic;

    const specs = [
      { title: `${topic} ${angle.style}`, q: base },
      { title: `${topic} - ${angle.len}`, q: `${base} full course tutorial` },
      { title: `${topic} - lecture series`, q: `${base} lecture series ${pickChannels(subjectText, topic)[0]}` },
      { title: `${topic} - visual explanation`, q: `${base} animation visualization` },
    ];

    const tech = detectTech(subject)[0];
    if (tech) {
      const channel = pickChannels(subjectText, topic)[0];
      specs.push({ title: `${tech.label} - ${channel}`, q: `${base} ${channel}` });
    }

    return dedupe(
      specs.map((s) => {
        const entry = item(s.title, `${YT_SEARCH}${enc(s.q)}`, "YouTube", "search");
        if (entry) entry.query = s.q;
        return entry;
      })
    );
  }

  function buildDocs(subjectText, topicName, description) {
    const subject = String(subjectText || "").trim();
    const topic = String(topicName || "").trim();
    const q = subject ? `${topic} ${subject}` : topic;
    const list = [];

    detectTech(subject).slice(0, 3).forEach((tech) => {
      list.push(
        item(tech.refTitle, tech.refUrl, tech.refUrl.includes("mozilla") ? "MDN" : hostOf(tech.refUrl), "curated"),
        item(`${tech.label} learning path`, tech.learnUrl, hostOf(tech.learnUrl), "curated")
      );
      tech.sites.forEach((id) => {
        const site = byId(id);
        if (site) list.push(item(`${topic} on ${site.name}`, site.search(q), site.name, "search"));
      });
    });

    if (!list.length) {
      ["gfg", "tutorialspoint", "wikipedia"].forEach((id) => {
        const site = byId(id);
        list.push(item(`${topic} on ${site.name}`, site.search(q), site.name, "search"));
      });
    }

    if (!list.some((r) => r && r.source === "Wikipedia")) {
      const hint = String(description || "").trim();
      if (hint) list.push(item(`Overview: ${hint}`, WIKI_SEARCH + enc(topic), "Wikipedia", "search"));
    }
    return dedupe(list).slice(0, 8);
  }

  function buildSearches(subjectText, topicName) {
    const subject = String(subjectText || "").trim();
    const topic = String(topicName || "").trim();
    const q = subject ? `${topic} ${subject} tutorial` : `${topic} tutorial`;
    return dedupe([
      item(`W3Schools: ${topic}`, `${GOOGLE}${enc(q + " site:w3schools.com")}`, "W3Schools", "search"),
      item(`GeeksforGeeks: ${topic}`, `${GOOGLE}${enc(q + " site:geeksforgeeks.org")}`, "GeeksforGeeks", "search"),
      item(`MDN: ${topic}`, `${GOOGLE}${enc(q + " site:developer.mozilla.org")}`, "MDN", "search"),
      item(`TutorialsPoint: ${topic}`, `${GOOGLE}${enc(q + " site:tutorialspoint.com")}`, "TutorialsPoint", "search"),
      item(`Wikipedia: ${topic}`, WIKI_SEARCH + enc(topic), "Wikipedia", "search"),
      item(`Free courses: ${topic}`, `https://www.coursera.org/search?query=${enc(q)}`, "Coursera", "search"),
    ]);
  }

  function hostOf(url) {
    try {
      return new URL(url).hostname.replace(/^www\./, "");
    } catch {
      return "Reference";
    }
  }

  const API = {
    forTopic(topic) {
      const t = topic || {};
      const subjectText = String(t.subjectName || "").trim();
      return {
        videos: buildVideos(subjectText, t.topicName, t.difficultyLevel, t.estimatedHours),
        docs: buildDocs(subjectText, t.topicName, t.description),
        searches: buildSearches(subjectText, t.topicName),
      };
    },
  };

  window.TopicResources = API;
})();
