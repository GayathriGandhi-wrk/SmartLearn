# -*- coding: utf-8 -*-
"""
Question Bank Taxonomy - SINGLE SOURCE OF TRUTH for category/subject/topic IDs.

Structure: Category -> Subject -> Topic -> Difficulty (BEGINNER/INTERMEDIATE/ADVANCED)

10 categories, 70 subjects (5 each), 350 topics (5 each).
Categories 1-8 cover computing; 9-10 cover Biology and Commerce departments.
IDs here MUST exactly match database/seed_base.sql.
"""

CATEGORIES = [
    (1, "Programming & Software Development",
     "Languages, programming concepts, syntax and software construction skills."),
    (2, "Database Management",
     "Design, querying, transactions, indexing and administration of databases."),
    (3, "Computer Networks",
     "Protocols, addressing, routing, wireless networks and network security."),
    (4, "Operating Systems & System Programming",
     "Processes, memory, file systems, Linux and low-level system software."),
    (5, "Artificial Intelligence & Data Science",
     "AI/ML algorithms, deep learning, statistics, NLP and data analytics."),
    (6, "Web & Mobile Development",
     "Frontend, backend, REST APIs, mobile apps and web technologies."),
    (7, "Cloud, Cybersecurity & DevOps",
     "Cloud services, security, CI/CD, containers, Kubernetes and IaC."),
    (8, "Core Computer Science & Emerging Technologies",
     "DS/Algorithms, discrete math, software engineering, architecture and futuristic tech."),
    (9, "Biology & Life Sciences",
     "Cell biology, physiology, genetics, ecology, microbiology and biotechnology."),
    (10, "Commerce, Accounting & Business Studies",
     "Accounting, business economics, management, marketing, banking, taxation and law."),
]

# (subject_id, category_id, subject_code, subject_name, department, credit_hours, description)
SUBJECTS = [
    # --- Category 1: Programming & Software Development ---
    (1,  1, "PROG101", "C Programming",                   "Computer Science", 3.0, "Fundamentals of C: syntax, pointers, memory and program structure."),
    (2,  1, "PROG102", "C++ Programming",                 "Computer Science", 3.0, "Object oriented programming with C++: classes, STL, templates and files."),
    (3,  1, "PROG103", "Java Programming",                "Computer Science", 3.0, "Java syntax, OOP, collections, streams and exception handling."),
    (4,  1, "PROG104", "Python Programming",              "Computer Science", 3.0, "Python basics, data structures, OOP, modules and file handling."),
    (5,  1, "PROG105", "JavaScript",                      "Computer Science", 3.0, "JavaScript fundamentals, functions, DOM, ES6 and async programming."),

    # --- Category 2: Database Management ---
    (6,  2, "DBMS101", "DBMS Fundamentals",               "Information Technology", 3.0, "ER model, relational model, normalization, transactions and indexing."),
    (7,  2, "DBMS102", "Structured Query Language (SQL)", "Information Technology", 3.0, "DDL, DML, joins, subqueries, aggregation and set operations."),
    (8,  2, "DBMS103", "MySQL",                           "Information Technology", 3.0, "MySQL data types, procedures, triggers, views and performance tuning."),
    (9,  2, "DBMS104", "PostgreSQL",                      "Information Technology", 3.0, "PostgreSQL types, JSON, window functions, PL/pgSQL and extensions."),
    (10, 2, "DBMS105", "MongoDB",                         "Information Technology", 3.0, "NoSQL concepts, documents, aggregation pipeline, replication and sharding."),

    # --- Category 3: Computer Networks ---
    (11, 3, "NET101", "Networking Fundamentals",          "Computer Engineering", 3.0, "OSI and TCP/IP models, physical/data-link layers and error control."),
    (12, 3, "NET102", "Network Protocols",                "Computer Engineering", 3.0, "IP addressing, subnetting, TCP/UDP, HTTP, DNS, ARP and ICMP."),
    (13, 3, "NET103", "Routing & Switching",              "Computer Engineering", 3.0, "Switching, VLANs, routing protocols, NAT and network devices."),
    (14, 3, "NET104", "Wireless & Mobile Networks",       "Computer Engineering", 3.0, "Wi-Fi, cellular generations, Bluetooth, PAN and wireless security."),
    (15, 3, "NET105", "Network Security",                 "Computer Engineering", 3.0, "Firewalls, cryptography, VPNs, attacks and security protocols."),

    # --- Category 4: Operating Systems & System Programming ---
    (16, 4, "OS101",  "Operating System Fundamentals",    "Computer Science", 3.0, "Processes, threads, scheduling, synchronization, deadlocks and memory."),
    (17, 4, "OS102",  "Memory, Storage & File Systems",   "Computer Science", 3.0, "Virtual memory, paging, file systems, disk and I/O scheduling."),
    (18, 4, "OS103",  "Linux & Shell Scripting",          "Computer Science", 3.0, "Linux commands, permissions, shell scripting and process control."),
    (19, 4, "OS104",  "System Programming",               "Computer Science", 3.0, "System calls, IPC, signals, interrupts, drivers and compilers."),
    (20, 4, "OS105",  "Modern Operating Systems",         "Computer Science", 3.0, "Windows internals, security, mobile OS, virtualization and containers."),

    # --- Category 5: Artificial Intelligence & Data Science ---
    (21, 5, "AI101",  "Artificial Intelligence Basics",   "AI & Data Science", 3.0, "Intelligent agents, search algorithms, knowledge representation and ML intro."),
    (22, 5, "AI102",  "Machine Learning",                 "AI & Data Science", 3.0, "Regression, classification, trees, ensembles, clustering and evaluation."),
    (23, 5, "AI103",  "Deep Learning",                    "AI & Data Science", 3.0, "Neural networks, backpropagation, CNN, RNN and frameworks."),
    (24, 5, "AI104",  "Data Science",                     "AI & Data Science", 3.0, "Data cleaning, visualization, statistics, EDA and big data tools."),
    (25, 5, "AI105",  "NLP & Computer Vision",            "AI & Data Science", 3.0, "Tokenization, text features, image processing and CV applications."),

    # --- Category 6: Web & Mobile Development ---
    (26, 6, "WEB101", "HTML & CSS",                       "Computer Science", 3.0, "HTML structure, forms, CSS selectors, layouts, flexbox and responsive design."),
    (27, 6, "WEB102", "Frontend JavaScript Frameworks",   "Computer Science", 3.0, "DOM, React, components, props, state, hooks and routing."),
    (28, 6, "WEB103", "Backend Development",              "Computer Science", 3.0, "Node.js, Express, REST APIs, auth, middleware and backend security."),
    (29, 6, "WEB104", "Mobile App Development",           "Computer Science", 3.0, "Android, iOS, Flutter, Dart, mobile UI and app deployment."),
    (30, 6, "WEB105", "Web Technologies & APIs",          "Computer Science", 3.0, "HTTP/REST, JSON, AJAX, storage, cookies, websockets and web security."),

    # --- Category 7: Cloud, Cybersecurity & DevOps ---
    (31, 7, "CLD101", "Cloud Computing",                  "Information Technology", 3.0, "Cloud models, AWS/Azure/GCP services, cloud storage and architecture."),
    (32, 7, "CLD102", "Cybersecurity",                    "Information Technology", 3.0, "Threats, IAM, malware defense, audits and compliance."),
    (33, 7, "CLD103", "DevOps & CI/CD",                   "Information Technology", 3.0, "DevOps practices, git, Docker, Kubernetes, monitoring and CI/CD."),
    (34, 7, "CLD104", "Cloud & Network Security",         "Information Technology", 3.0, "Cloud security, encryption, scanning, incident response and privacy."),
    (35, 7, "CLD105", "Infrastructure & Automation",      "Information Technology", 3.0, "IaC, Terraform, CloudFormation, config management, pipelines and SRE."),

    # --- Category 8: Core Computer Science & Emerging Technologies ---
    (36, 8, "CORE101", "Data Structures & Algorithms",    "Computer Science", 4.0, "Lists, stacks, queues, trees, graphs, sorting, searching and complexity."),
    (37, 8, "CORE102", "Discrete Mathematics",            "Computer Science", 3.0, "Sets, logic, relations, combinatorics, graph theory and number theory."),
    (38, 8, "CORE103", "Software Engineering",            "Computer Science", 3.0, "SDLC, requirements, design, testing, patterns, agile and project management."),
    (39, 8, "CORE104", "Computer Architecture",           "Computer Science", 3.0, "CPU, memory hierarchy, pipelining, I/O and parallel architecture."),
    (40, 8, "CORE105", "Emerging Technologies",           "Computer Science", 3.0, "Blockchain, IoT, quantum computing, edge/5G and AR/VR."),

    # --- Foundation courses ---
    (41, 8, "MTH101", "Engineering Mathematics I",        "Computer Science", 3.0, "Limits, continuity, differentiation, integration, matrices and differential equations."),
    (42, 8, "PHY101", "Applied Physics & Basic Electronics","Computer Science", 3.0, "Semiconductors, diodes, transistors, digital logic, sequential circuits and microprocessors."),
    (43, 8, "ENG101", "Communication Skills & Technical English","Computer Science", 2.0, "Grammar, technical writing, listening, speaking, presentation and professional communication."),
    (44, 1, "PROG106", "Computer Fundamentals & Programming Basics","Computer Science", 3.0, "Hardware, number systems, boolean logic, algorithms, flowcharts and C programming basics."),
    (45, 8, "CS101", "Fundamentals of Computing",         "Computer Science", 3.0, "Computer organization, software, internet, data representation and computing trends."),

    # --- Advanced core ---
    (46, 8, "CORE106", "Compiler Design",                 "Computer Science", 3.0, "Lexical analysis, parsing, semantic analysis, intermediate code, code generation and optimization."),
    (47, 3, "NET106", "Distributed Systems",              "Computer Engineering", 3.0, "Architectures, communication, RPC, consistency, replication, fault tolerance and consensus."),
    (48, 8, "CORE107", "Software Testing & Quality Assurance","Computer Science", 3.0, "Test design, automation, CI/CD, quality metrics, defect management and reporting."),
    (49, 2, "DBMS106", "Data Warehousing & Data Mining",  "Information Technology", 3.0, "Warehouse architecture, ETL, OLAP, mining techniques, association and clustering."),
    (50, 8, "CORE108", "Computer Graphics",               "Computer Science", 3.0, "Primitives, 2D/3D transformations, projections, shading, rendering and animation."),

    # --- Electives & project ---
    (51, 5, "AI106", "Big Data Analytics",                "AI & Data Science", 3.0, "Big data fundamentals, Hadoop, HDFS, MapReduce, Spark, streaming, NoSQL and visualization."),
    (52, 7, "CLD106", "Blockchain & Cryptocurrency",      "Information Technology", 3.0, "Distributed ledgers, cryptography, consensus, smart contracts, platforms and DApps."),
    (53, 7, "CLD107", "Cyber Forensics & Incident Response","Information Technology", 3.0, "Digital evidence, chain of custody, disk/network/memory forensics, tools and incident response."),
    (54, 8, "CORE109", "Project Work - Phase I",          "Computer Science", 3.0, "Problem identification, literature survey, requirement analysis, system design and documentation."),
    (55, 5, "AI107", "Advanced Machine Learning",         "AI & Data Science", 3.0, "Ensembles, deep networks, CNN/RNN, reinforcement learning, transformers and model deployment."),

    # --- Capstone ---
    (56, 8, "CORE110", "Capstone Project",                "Computer Science", 6.0, "Full project lifecycle: planning, implementation, testing, deployment, demo and defense."),
    (57, 8, "CORE111", "Professional Ethics & IPR",       "Computer Science", 2.0, "Professional ethics, intellectual property rights, cyber law, privacy and corporate responsibility."),
    (58, 7, "CLD108", "Cloud Security & Governance",      "Information Technology", 3.0, "Shared responsibility, IAM, compliance, auditing, zero trust and cloud incident response."),
    (59, 6, "WEB106", "Advanced Web Technologies",        "Computer Science", 3.0, "Microservices, containerization, GraphQL, WebSockets, PWAs and edge deployment."),
    (60, 8, "CORE112", "Emerging Research in Computing",  "Computer Science", 3.0, "Quantum computing, edge AI, metaverse, AR/VR, green computing and research methodology."),

    # --- Category 9: Biology & Life Sciences (department: Biology) ---
    (61, 9, "BIO101", "Cell Biology & Biochemistry",       "Biology", 4.0, "Cell structure, membranes, transport, enzymes, metabolism, photosynthesis and respiration."),
    (62, 9, "BIO102", "Human Anatomy & Physiology",       "Biology", 4.0, "Tissues, organ systems, skeleton, muscles, circulation, respiration, digestion and nervous system."),
    (63, 9, "BIO103", "Genetics & Molecular Biology",     "Biology", 4.0, "DNA replication, transcription, translation, Mendelian genetics, regulation and genetic engineering."),
    (64, 9, "BIO104", "Ecology & Environmental Biology",  "Biology", 3.0, "Ecosystems, energy flow, food chains, population dynamics, biomes, biodiversity and conservation."),
    (65, 9, "BIO105", "Microbiology & Biotechnology",     "Biology", 4.0, "Microbial diversity, bacterial growth, pathogens, immunity, recombinant DNA and industrial biotechnology."),

    # --- Category 10: Commerce, Accounting & Business Studies (department: Commerce) ---
    (66, 10, "COM101", "Financial Accounting",             "Commerce", 4.0, "Accounting principles, journals, ledgers, trial balance, error rectification, valuation and final accounts."),
    (67, 10, "COM102", "Business Economics & Management",  "Commerce", 3.0, "Business organisation, management functions, planning, structure, leadership, control and decision making."),
    (68, 10, "COM103", "Cost Accounting & Budgeting",      "Commerce", 4.0, "Cost concepts, cost sheets, classification, variance analysis, standard and marginal costing, budgeting."),
    (69, 10, "COM104", "Marketing & Sales Management",     "Commerce", 3.0, "Marketing concepts, consumer behaviour, product and brand decisions, pricing, distribution, promotion and CRM."),
    (70, 10, "COM105", "Banking, Taxation & Corporate Law","Commerce", 4.0, "Banking functions, instruments, accounts, regulation, direct and indirect tax, contracts and corporate law."),
]

# (topic_id, subject_id, topic_name, difficulty_level, description, estimated_hours)
TOPICS = [
    # Subject 1: C Programming
    (1,   1, "Fundamentals of C",            "BEGINNER",     "Basic syntax, data types, operators and program structure.", 2),
    (2,   1, "Control Structures & Loops",   "BEGINNER",     "Conditional statements and iteration constructs in C.", 2),
    (3,   1, "Functions & Recursion",        "INTERMEDIATE", "Function declaration, scope, parameters and recursion.", 3),
    (4,   1, "Pointers & Memory Management", "ADVANCED",     "Pointers, dynamic allocation and memory lifetime.", 3),
    (5,   1, "Arrays & Strings",             "INTERMEDIATE", "Array manipulation, string handling and multi-dimensional arrays.", 3),
    # Subject 2: C++
    (6,   2, "Classes & Objects",            "BEGINNER",     "Class design, access specifiers and object creation.", 2),
    (7,   2, "Constructors & Destructors",   "BEGINNER",     "Constructors, initialization lists and object lifetime.", 2),
    (8,   2, "Inheritance & Polymorphism",   "INTERMEDIATE", "Derived classes, virtual functions and abstract types.", 3),
    (9,   2, "STL & Templates",              "INTERMEDIATE", "Containers, algorithms, iterators and function/class templates.", 3),
    (10,  2, "Operator Overloading & Files", "ADVANCED",     "Operator functions, stream I/O and file handling.", 3),
    # Subject 3: Java
    (11,  3, "Java Basics & Variables",      "BEGINNER",     "JVM, primitive types, operators and variable scoping.", 2),
    (12,  3, "Control Flow & Loops",         "BEGINNER",     "Decision statements and iteration in Java.", 2),
    (13,  3, "Object Oriented Programming",  "INTERMEDIATE", "Classes, inheritance, interfaces, polymorphism and abstraction.", 3),
    (14,  3, "Java Collections Framework",   "INTERMEDIATE", "List, Set, Map, sorting and collection utilities.", 3),
    (15,  3, "Exception Handling & Streams", "ADVANCED",     "Exceptions, try-catch, lambda expressions and streams API.", 3),
    # Subject 4: Python
    (16,  4, "Python Basics & Data Types",   "BEGINNER",     "Variables, numbers, strings, booleans and type conversion.", 2),
    (17,  4, "Control Flow & Functions",     "BEGINNER",     "Conditionals, loops, function definitions and arguments.", 2),
    (18,  4, "Python Data Structures",       "INTERMEDIATE", "Lists, tuples, dictionaries, sets and comprehensions.", 3),
    (19,  4, "Object Oriented Python",       "INTERMEDIATE", "Classes, inheritance, magic methods and decorators.", 3),
    (20,  4, "Modules, Files & Advanced",    "ADVANCED",     "Modules, file I/O, exceptions, generators and libraries.", 3),
    # Subject 5: JavaScript
    (21,  5, "JS Fundamentals & Variables",  "BEGINNER",     "Variables, data types, operators and strict mode.", 2),
    (22,  5, "Functions & Scope",            "BEGINNER",     "Function declarations, closures, hoisting and this binding.", 2),
    (23,  5, "DOM & Events",                 "INTERMEDIATE", "Document Object Model, event handling and event bubbling.", 3),
    (24,  5, "ES6 & Arrays",                 "INTERMEDIATE", "Arrow functions, destructuring, spread, array methods and modules.", 3),
    (25,  5, "Async JavaScript",             "ADVANCED",     "Promises, async/await, callbacks and the event loop.", 3),
    # Subject 6: DBMS Fundamentals
    (26,  6, "ER Model",                     "BEGINNER",     "Entities, attributes, relationships and ER diagrams.", 2),
    (27,  6, "Relational Model & Keys",      "BEGINNER",     "Tables, tuples, primary keys, foreign keys and constraints.", 2),
    (28,  6, "Normalization",                "INTERMEDIATE", "Functional dependencies, 1NF-3NF and BCNF.", 3),
    (29,  6, "Transactions & Concurrency",   "INTERMEDIATE", "ACID, isolation levels, locking and serializability.", 3),
    (30,  6, "Indexing & Query Optimization","ADVANCED",     "Index structures, query plans, cost estimation and tuning.", 3),
    # Subject 7: SQL
    (31,  7, "SQL Basics & DDL",             "BEGINNER",     "SELECT statements and Data Definition Language commands.", 2),
    (32,  7, "DML & CRUD Operations",        "BEGINNER",     "INSERT, UPDATE, DELETE and data manipulation.", 2),
    (33,  7, "Joins",                        "INTERMEDIATE", "INNER, LEFT, RIGHT, FULL and CROSS joins.", 3),
    (34,  7, "Subqueries & Set Operations",  "INTERMEDIATE", "Nested queries, UNION, INTERSECT and correlated subqueries.", 3),
    (35,  7, "Aggregation & Grouping",       "ADVANCED",     "GROUP BY, HAVING, aggregate functions and window functions.", 3),
    # Subject 8: MySQL
    (36,  8, "MySQL Data Types & Tables",    "BEGINNER",     "Numeric, string, date/time types and table creation.", 2),
    (37,  8, "MySQL Queries & Constraints",  "BEGINNER",     "Querying with constraints, defaults and keys.", 2),
    (38,  8, "Stored Procedures & Functions","INTERMEDIATE", "Delimiters, parameters, cursors and functions.", 3),
    (39,  8, "Triggers & Views",             "INTERMEDIATE", "Trigger timing, events and view creation.", 3),
    (40,  8, "MySQL Performance Tuning",     "ADVANCED",     "EXPLAIN, indexes, caching and slow query analysis.", 3),
    # Subject 9: PostgreSQL
    (41,  9, "PostgreSQL Basics",            "BEGINNER",     "psql, databases, schemas and roles.", 2),
    (42,  9, "Data Types & Constraints",     "BEGINNER",     "Numeric, text, UUID, enum and table constraints.", 2),
    (43,  9, "JSON & Array Support",         "INTERMEDIATE", "jsonb, array types and operator usage.", 3),
    (44,  9, "Window Functions",             "INTERMEDIATE", "PARTITION BY, ranking and aggregate windows.", 3),
    (45,  9, "PL/pgSQL & Extensions",        "ADVANCED",     "Functions, triggers, CTEs, recursive queries and extensions.", 3),
    # Subject 10: MongoDB
    (46, 10, "NoSQL & MongoDB Basics",       "BEGINNER",     "Document model, BSON, databases, collections.", 2),
    (47, 10, "Documents & CRUD",             "BEGINNER",     "Insert, find, update, delete and query operators.", 2),
    (48, 10, "Aggregation Pipeline",         "INTERMEDIATE", "match, group, project, sort, lookup stages.", 3),
    (49, 10, "Indexing & Performance",       "INTERMEDIATE", "Single, compound, text indexes and query plans.", 3),
    (50, 10, "Replication & Sharding",       "ADVANCED",     "Replica sets, sharding, consistency and durability.", 3),
    # Subject 11: Networking Fundamentals
    (51, 11, "OSI Model",                    "BEGINNER",     "Seven layers and their responsibilities.", 2),
    (52, 11, "TCP/IP Model",                 "BEGINNER",     "Four layers and protocol mapping.", 2),
    (53, 11, "Physical Layer & Topologies",  "INTERMEDIATE", "Signals, media types and network topologies.", 3),
    (54, 11, "Data Link Layer & MAC",        "INTERMEDIATE", "Frames, MAC addressing, switches and bridging.", 3),
    (55, 11, "Error Detection & Correction", "ADVANCED",     "Parity, CRC, checksums and Hamming codes.", 3),
    # Subject 12: Network Protocols
    (56, 12, "IP Addressing & Subnetting",   "BEGINNER",     "IPv4 addresses, classes and subnet masks.", 2),
    (57, 12, "IPv6",                         "BEGINNER",     "128-bit addressing, notation and transitions.", 2),
    (58, 12, "TCP & UDP",                    "INTERMEDIATE", "Connection setup, ports, reliability vs speed.", 3),
    (59, 12, "HTTP & DNS",                   "INTERMEDIATE", "HTTP methods, status codes and DNS resolution.", 3),
    (60, 12, "ARP & ICMP",                   "ADVANCED",     "Address resolution and control messages.", 3),
    # Subject 13: Routing & Switching
    (61, 13, "Switching Basics & VLANs",     "BEGINNER",     "Switch forwarding, VLANs and trunking.", 2),
    (62, 13, "Routing & RIP",                "BEGINNER",     "Routing tables, static/dynamic routes, distance vector.", 2),
    (63, 13, "OSPF & Link State",            "INTERMEDIATE", "Link state databases and areas.", 3),
    (64, 13, "BGP & EIGRP",                  "INTERMEDIATE", "Path vector routing and inter-AS routing.", 3),
    (65, 13, "NAT & Network Devices",        "ADVANCED",     "NAT/PAT, routers, gateways and device management.", 3),
    # Subject 14: Wireless & Mobile Networks
    (66, 14, "Wireless LAN & Wi-Fi",         "BEGINNER",     "802.11 standards, SSID and access points.", 2),
    (67, 14, "Cellular Generations",         "BEGINNER",     "1G to 5G evolution and their features.", 2),
    (68, 14, "Bluetooth & PAN",              "INTERMEDIATE", "Bluetooth classes, piconets and pairing.", 3),
    (69, 14, "Mobile IP & Handover",         "INTERMEDIATE", "Mobile IP addressing and handoff mechanisms.", 3),
    (70, 14, "Wireless Security",            "ADVANCED",     "WEP/WPA/WPA2, 802.1X and rogue access points.", 3),
    # Subject 15: Network Security
    (71, 15, "Firewalls & IDS/IPS",          "BEGINNER",     "Packet filtering, stateful inspection and intrusion systems.", 2),
    (72, 15, "Cryptography Basics",          "BEGINNER",     "Symmetric/ASymmetric ciphers and hashing.", 2),
    (73, 15, "VPN & Tunneling",              "INTERMEDIATE", "IPsec, GRE, SSL VPN and tunnel modes.", 3),
    (74, 15, "Network Attacks & Threats",    "INTERMEDIATE", "DDoS, MITM, spoofing and sniffing.", 3),
    (75, 15, "Security Protocols",           "ADVANCED",     "SSL/TLS, IPsec and secure application protocols.", 3),
    # Subject 16: OS Fundamentals
    (76, 16, "Processes & Threads",          "BEGINNER",     "Process states, PCB, threads and context switching.", 2),
    (77, 16, "CPU Scheduling",               "BEGINNER",     "FCFS, SJF, Round Robin and priority scheduling.", 2),
    (78, 16, "Process Synchronization",      "INTERMEDIATE", "Critical sections, semaphores, mutexes and monitors.", 3),
    (79, 16, "Deadlocks",                    "INTERMEDIATE", "Necessary conditions, avoidance and detection.", 3),
    (80, 16, "Memory Management",            "ADVANCED",     "Paging, segmentation and allocation strategies.", 3),
    # Subject 17: Memory, Storage & File Systems
    (81, 17, "Virtual Memory & Paging",      "BEGINNER",     "Page tables, TLB, demand paging and page faults.", 2),
    (82, 17, "Segmentation",                 "BEGINNER",     "Segment tables and address translation.", 2),
    (83, 17, "File Systems",                 "INTERMEDIATE", "Directories, allocation methods and inodes.", 3),
    (84, 17, "Disk Scheduling",              "INTERMEDIATE", "FCFS, SSTF, SCAN and C-SCAN algorithms.", 3),
    (85, 17, "I/O Systems & DMA",            "ADVANCED",     "Polling, interrupts, DMA and buffering.", 3),
    # Subject 18: Linux & Shell Scripting
    (86, 18, "Linux Basics & Commands",      "BEGINNER",     "Filesystem hierarchy and common commands.", 2),
    (87, 18, "Permissions & Ownership",      "BEGINNER",     "chmod, chown, umask and ACLs.", 2),
    (88, 18, "Shell Scripting",              "INTERMEDIATE", "Variables, conditionals, loops and functions in bash.", 3),
    (89, 18, "Process Management in Linux",  "INTERMEDIATE", "ps, top, kill, jobs and systemd services.", 3),
    (90, 18, "Networking & Package Mgmt",    "ADVANCED",     "ip, ssh, apt/yum/dnf and system administration.", 3),
    # Subject 19: System Programming
    (91, 19, "System Calls & Libraries",     "BEGINNER",     "User/kernel modes and common system calls.", 2),
    (92, 19, "Signals & IPC",                "BEGINNER",     "Signals, pipes, message queues and shared memory.", 2),
    (93, 19, "Interrupts & Exceptions",      "INTERMEDIATE", "Interrupt vectors, handlers and ISRs.", 3),
    (94, 19, "Device Drivers",               "INTERMEDIATE", "Driver models, kernel modules and device I/O.", 3),
    (95, 19, "Assemblers & Compilers",       "ADVANCED",     "Lexing, parsing, assembly and linking.", 3),
    # Subject 20: Modern Operating Systems
    (96, 20, "Windows Architecture",         "BEGINNER",     "NT kernel, HAL and user/kernel modes.", 2),
    (97, 20, "Windows Process & Memory",     "BEGINNER",     "Jobs, sessions and virtual address layout.", 2),
    (98, 20, "Windows Security & Registry",  "INTERMEDIATE", "ACLs, SIDs, services and registry.", 3),
    (99, 20, "Mobile & macOS Operating Sys", "INTERMEDIATE", "Android/iOS/macOS kernel and app sandboxing.", 3),
    (100,20, "Virtualization & Containers",  "ADVANCED",     "Hypervisors, VMs, containers and namespaces.", 3),
    # Subject 21: AI Basics
    (101,21, "AI & Intelligent Agents",      "BEGINNER",     "Agent types, environments and rationality.", 2),
    (102,21, "Search Algorithms",            "BEGINNER",     "BFS, DFS, heuristic and A* search.", 2),
    (103,21, "Knowledge Representation",     "INTERMEDIATE", "Logic, rules, frames and ontologies.", 3),
    (104,21, "Intro to Machine Learning",    "INTERMEDIATE", "Supervised, unsupervised and reinforcement learning.", 3),
    (105,21, "AI Ethics & Applications",     "ADVANCED",     "Bias, fairness, explainability and AI systems.", 3),
    # Subject 22: Machine Learning
    (106,22, "Linear & Logistic Regression", "BEGINNER",     "Cost functions, gradient descent and interpretation.", 2),
    (107,22, "Classification & k-NN",        "BEGINNER",     "Decision boundaries, distance metrics and neighbors.", 2),
    (108,22, "Decision Trees & Ensembles",   "INTERMEDIATE", "Splitting criteria, random forests and boosting.", 3),
    (109,22, "Clustering & Dimensionality",  "INTERMEDIATE", "k-means, hierarchical, PCA and t-SNE.", 3),
    (110,22, "Evaluation & Overfitting",     "ADVANCED",     "Accuracy, precision, recall, cross-validation and regularization.", 3),
    # Subject 23: Deep Learning
    (111,23, "Neural Network Basics",        "BEGINNER",     "Perceptrons, layers, weights and biases.", 2),
    (112,23, "Backpropagation & Activations","BEGINNER",     "Gradients, activation functions and learning rate.", 2),
    (113,23, "Convolutional Neural Networks", "INTERMEDIATE", "Convolutions, pooling, filters and feature maps.", 3),
    (114,23, "RNNs & Sequence Models",       "INTERMEDIATE", "Recurrence, LSTM, GRU and attention.", 3),
    (115,23, "Frameworks & Training",        "ADVANCED",     "TensorFlow/PyTorch, regularization, transfer learning.", 3),
    # Subject 24: Data Science
    (116,24, "Data Collection & Cleaning",   "BEGINNER",     "Sources, missing values and outliers.", 2),
    (117,24, "Data Visualization",           "BEGINNER",     "Charts, plots and effective visualization.", 2),
    (118,24, "Statistical Analysis",         "INTERMEDIATE", "Distributions, hypothesis tests and confidence intervals.", 3),
    (119,24, "Exploratory Data Analysis",    "INTERMEDIATE", "Summary stats, correlation and feature analysis.", 3),
    (120,24, "Big Data & MLOps",             "ADVANCED",     "Hadoop, Spark, pipelines and model deployment.", 3),
    # Subject 25: NLP & Computer Vision
    (121,25, "NLP Basics & Tokenization",    "BEGINNER",     "Corpora, tokens, stopwords and segmentation.", 2),
    (122,25, "Text Processing & Features",   "BEGINNER",     "Bag-of-words, TF-IDF and n-grams.", 2),
    (123,25, "Computer Vision Basics",       "INTERMEDIATE", "Pixels, color models and image representation.", 3),
    (124,25, "Image Processing & Features",  "INTERMEDIATE", "Filters, edge detection and feature extraction.", 3),
    (125,25, "CV & NLP Applications",        "ADVANCED",     "Object detection, sentiment analysis and transformers.", 3),
    # Subject 26: HTML & CSS
    (126,26, "HTML Fundamentals",            "BEGINNER",     "Tags, attributes, semantic elements and document structure.", 2),
    (127,26, "Forms & Multimedia",           "BEGINNER",     "Inputs, validation, audio and video.", 2),
    (128,26, "CSS Basics & Selectors",       "INTERMEDIATE", "Selectors, specificity, box model and colors.", 3),
    (129,26, "CSS Layouts & Flexbox",        "INTERMEDIATE", "Flexbox, grid and positioning.", 3),
    (130,26, "Responsive Design & Animations","ADVANCED",    "Media queries, breakpoints, transforms and transitions.", 3),
    # Subject 27: Frontend JavaScript Frameworks
    (131,27, "DOM Manipulation",             "BEGINNER",     "Selectors, traversal and attribute updates.", 2),
    (132,27, "React Fundamentals",           "BEGINNER",     "JSX, components and virtual DOM.", 2),
    (133,27, "Components & Props",           "INTERMEDIATE", "Props drilling, composition and reusability.", 3),
    (134,27, "State & Hooks",                "INTERMEDIATE", "useState, useEffect, context and state management.", 3),
    (135,27, "Routing & Performance",        "ADVANCED",     "React Router, lazy loading and memoization.", 3),
    # Subject 28: Backend Development
    (136,28, "Node.js Basics",               "BEGINNER",     "Event loop, npm and module system.", 2),
    (137,28, "Express & REST APIs",          "BEGINNER",     "Routes, middleware and JSON responses.", 2),
    (138,28, "Authentication & Authorization","INTERMEDIATE", "JWT, sessions, OAuth and password hashing.", 3),
    (139,28, "Middleware & Error Handling",  "INTERMEDIATE", "Middleware chains, validation and error handlers.", 3),
    (140,28, "Backend Security & Scaling",   "ADVANCED",     "Rate limiting, injection, CORS and caching.", 3),
    # Subject 29: Mobile App Development
    (141,29, "Mobile App Basics",            "BEGINNER",     "App types, lifecycle and stores.", 2),
    (142,29, "Android Fundamentals",         "BEGINNER",     "Activities, intents, layouts and manifests.", 2),
    (143,29, "iOS & Swift Basics",           "INTERMEDIATE", "Swift syntax, view controllers and storyboards.", 3),
    (144,29, "Flutter & Dart",               "INTERMEDIATE", "Widgets, state and Dart language basics.", 3),
    (145,29, "Mobile UI & Deployment",       "ADVANCED",     "App signing, release builds and store submission.", 3),
    # Subject 30: Web Technologies & APIs
    (146,30, "HTTP & REST",                  "BEGINNER",     "Methods, status codes, headers and REST constraints.", 2),
    (147,30, "JSON & AJAX",                  "BEGINNER",     "JSON syntax, parse/stringify and fetch/XHR.", 2),
    (148,30, "Web Storage & Cookies",        "INTERMEDIATE", "localStorage, sessionStorage and cookie attributes.", 3),
    (149,30, "WebSockets & Real-time",       "INTERMEDIATE", "WebSocket handshake, frames and use cases.", 3),
    (150,30, "Web Security",                 "ADVANCED",     "XSS, CSRF, CORS, SQL injection and HTTPS.", 3),
    # Subject 31: Cloud Computing
    (151,31, "Cloud Models & Services",      "BEGINNER",     "IaaS, PaaS, SaaS and deployment models.", 2),
    (152,31, "AWS Core Services",            "BEGINNER",     "EC2, S3, Lambda, RDS and VPC.", 2),
    (153,31, "Azure & GCP Basics",           "INTERMEDIATE", "Azure VMs, functions, GCP compute and storage.", 3),
    (154,31, "Cloud Storage & Databases",    "INTERMEDIATE", "Object storage, scaling and managed databases.", 3),
    (155,31, "Architecture & Cost Mgmt",     "ADVANCED",     "Serverless, cost optimization and design principles.", 3),
    # Subject 32: Cybersecurity
    (156,32, "Security Fundamentals",        "BEGINNER",     "CIA triad, risk and defense in depth.", 2),
    (157,32, "Threats & Vulnerabilities",    "BEGINNER",     "Malware types, zero-days and attack vectors.", 2),
    (158,32, "Identity & Access Management", "INTERMEDIATE", "Authentication factors, SSO, MFA and RBAC.", 3),
    (159,32, "Malware & Defense",            "INTERMEDIATE", "Antivirus, EDR, sandboxing and hardening.", 3),
    (160,32, "Audits & Compliance",          "ADVANCED",     "Pen testing, standards, GDPR and ISO 27001.", 3),
    # Subject 33: DevOps & CI/CD
    (161,33, "DevOps Principles & CI/CD",    "BEGINNER",     "Culture, pipelines and delivery automation.", 2),
    (162,33, "Git & Version Control",        "BEGINNER",     "Repositories, branches, merging and workflows.", 2),
    (163,33, "Docker & Containers",          "INTERMEDIATE", "Images, containers, Dockerfile and registry.", 3),
    (164,33, "Kubernetes",                   "INTERMEDIATE", "Pods, deployments, services and ingress.", 3),
    (165,33, "Monitoring & Logging",         "ADVANCED",     "Prometheus, Grafana, ELK and observability.", 3),
    # Subject 34: Cloud & Network Security
    (166,34, "Cloud Security",               "BEGINNER",     "Shared responsibility and cloud best practices.", 2),
    (167,34, "Secure Communication",         "BEGINNER",     "TLS, certificates and encryption in transit.", 2),
    (168,34, "Security Tools & Scanning",    "INTERMEDIATE", "Nmap, Wireshark, vulnerability scanners.", 3),
    (169,34, "Incident Response",            "INTERMEDIATE", "Detection, containment, eradication and recovery.", 3),
    (170,34, "Data Protection & Privacy",    "ADVANCED",     "Encryption at rest, KMS and privacy laws.", 3),
    # Subject 35: Infrastructure & Automation
    (171,35, "Infrastructure as Code",       "BEGINNER",     "Declarative vs imperative and reproducibility.", 2),
    (172,35, "Terraform & CloudFormation",   "BEGINNER",     "Providers, resources, state and templates.", 2),
    (173,35, "Configuration Management",     "INTERMEDIATE", "Ansible, Puppet, Chef and idempotency.", 3),
    (174,35, "CI/CD Pipelines",              "INTERMEDIATE", "Stages, artifacts, approvals and environments.", 3),
    (175,35, "SRE & Reliability",            "ADVANCED",     "SLOs, SLIs, error budgets and incident metrics.", 3),
    # Subject 36: Data Structures & Algorithms
    (176,36, "Arrays & Linked Lists",        "BEGINNER",     "Static vs dynamic storage and operations.", 2),
    (177,36, "Stacks & Queues",              "BEGINNER",     "LIFO/FIFO and their applications.", 2),
    (178,36, "Trees & Graphs",               "INTERMEDIATE", "Binary trees, BST, traversals and graph representations.", 3),
    (179,36, "Sorting & Searching",          "INTERMEDIATE", "Sort algorithms, binary search and complexity.", 3),
    (180,36, "Algorithm Analysis & Design",  "ADVANCED",     "Big-O, divide-conquer, greedy and dynamic programming.", 3),
    # Subject 37: Discrete Mathematics
    (181,37, "Sets & Logic",                 "BEGINNER",     "Set operations, propositional logic and truth tables.", 2),
    (182,37, "Relations & Functions",        "BEGINNER",     "Types of relations, equivalence and function properties.", 2),
    (183,37, "Combinatorics",                "INTERMEDIATE", "Permutations, combinations and pigeonhole principle.", 3),
    (184,37, "Graph Theory",                 "INTERMEDIATE", "Graph types, paths, cycles and trees.", 3),
    (185,37, "Number Theory & Algebra",      "ADVANCED",     "Modular arithmetic, primes and Boolean algebra.", 3),
    # Subject 38: Software Engineering
    (186,38, "SDLC & Models",                "BEGINNER",     "Waterfall, Agile, iterative and V-model.", 2),
    (187,38, "Requirements & Design",        "BEGINNER",     "Functional/non-functional requirements and UML.", 2),
    (188,38, "Testing & Quality Assurance",  "INTERMEDIATE", "Unit, integration, system and acceptance testing.", 3),
    (189,38, "Design Patterns",              "INTERMEDIATE", "Creational, structural and behavioral patterns.", 3),
    (190,38, "Agile & Project Management",   "ADVANCED",     "Scrum, sprints, kanban and estimation.", 3),
    # Subject 39: Computer Architecture
    (191,39, "CPU & Instruction Cycle",      "BEGINNER",     "ALU, registers, control unit and fetch-decode-execute.", 2),
    (192,39, "Memory Hierarchy",             "BEGINNER",     "Cache, RAM, registers and locality.", 2),
    (193,39, "Pipelining & Hazards",         "INTERMEDIATE", "Pipeline stages, data/control hazards and stalls.", 3),
    (194,39, "I/O & Peripherals",            "INTERMEDIATE", "Programmed I/O, interrupt-driven I/O and buses.", 3),
    (195,39, "Parallel & Multicore",         "ADVANCED",     "SIMD, MIMD, threads and cache coherence.", 3),
    # Subject 40: Emerging Technologies
    (196,40, "Blockchain",                   "BEGINNER",     "Blocks, hashing, consensus and distributed ledgers.", 2),
    (197,40, "Internet of Things (IoT)",     "BEGINNER",     "Sensors, protocols and edge devices.", 2),
    (198,40, "Quantum Computing",            "INTERMEDIATE", "Qubits, superposition and entanglement.", 3),
    (199,40, "Edge & 5G",                    "INTERMEDIATE", "Edge computing, latency and network slicing.", 3),
    (200,40, "AR/VR & Metaverse",            "ADVANCED",     "Rendering, tracking, HMDs and applications.", 3),

    # Subject 41: Engineering Mathematics I
    (201,41, "Limits & Continuity",          "BEGINNER",     "Limits, continuity and standard limit results.", 2),
    (202,41, "Differentiation",              "BEGINNER",     "Derivatives, rules, and applications to tangents and extrema.", 2),
    (203,41, "Integration",                  "INTERMEDIATE", "Indefinite/definite integrals and techniques of integration.", 3),
    (204,41, "Matrices & Linear Systems",    "INTERMEDIATE", "Matrix operations, determinants, rank and linear equations.", 3),
    (205,41, "Differential Equations",       "ADVANCED",     "First/second order ODEs and applications.", 3),
    # Subject 42: Applied Physics & Basic Electronics
    (206,42, "Semiconductors & Diodes",      "BEGINNER",     "Energy bands, doping, p-n junction and diode characteristics.", 2),
    (207,42, "Transistors & Amplifiers",     "BEGINNER",     "BJT/FET operation and basic amplifier circuits.", 2),
    (208,42, "Digital Logic & Number Systems","INTERMEDIATE", "Binary/hex arithmetic and combinational logic gates.", 3),
    (209,42, "Sequential Circuits & Memories","INTERMEDIATE", "Flip-flops, counters, registers and memory basics.", 3),
    (210,42, "Microprocessor Fundamentals",  "ADVANCED",     "CPU architecture, instruction sets and interfacing.", 3),
    # Subject 43: Communication Skills & Technical English
    (211,43, "English Grammar & Vocabulary", "BEGINNER",     "Parts of speech, tenses, articles and word building.", 2),
    (212,43, "Technical Writing",            "BEGINNER",     "Reports, letters, e-mails and documentation style.", 2),
    (213,43, "Listening & Speaking",         "INTERMEDIATE", "Comprehension, pronunciation and conversational fluency.", 3),
    (214,43, "Presentation & Group Discussion","INTERMEDIATE", "Structuring talks, delivery and GD etiquette.", 3),
    (215,43, "Professional Communication",   "ADVANCED",     "Interviews, meetings and workplace communication.", 3),
    # Subject 44: Computer Fundamentals & Programming Basics
    (216,44, "Computer Hardware & Organization","BEGINNER", "Components, memory, storage and peripherals.", 2),
    (217,44, "Number Systems & Boolean Logic","BEGINNER",    "Binary/octal/hex conversion and logic gates.", 2),
    (218,44, "Algorithms & Flowcharts",      "INTERMEDIATE", "Problem solving, pseudocode and flow control.", 3),
    (219,44, "C Programming Basics",         "INTERMEDIATE", "Data types, operators, control structures and functions.", 3),
    (220,44, "Problem Solving & Debugging",  "ADVANCED",     "Tracing, testing and fixing logic errors.", 3),
    # Subject 45: Fundamentals of Computing
    (221,45, "Introduction to Computing",    "BEGINNER",     "History, generations and computing devices.", 2),
    (222,45, "Software & Operating Basics",  "BEGINNER",     "System/application software and OS roles.", 2),
    (223,45, "Internet & Web Basics",        "INTERMEDIATE", "Networks, browsers, e-mail and web technologies.", 3),
    (224,45, "Data Representation",          "INTERMEDIATE", "ASCII, Unicode, images, audio and encoding.", 3),
    (225,45, "Computing Trends & Careers",   "ADVANCED",     "AI, cloud, IoT and IT career pathways.", 3),
    # Subject 46: Compiler Design
    (226,46, "Lexical Analysis",             "BEGINNER",     "Tokens, regular expressions and scanners.", 2),
    (227,46, "Syntax Analysis & Parsing",    "BEGINNER",     "Context-free grammars and top-down/bottom-up parsing.", 2),
    (228,46, "Semantic Analysis & Symbol Table","INTERMEDIATE", "Type checking, scopes and symbol management.", 3),
    (229,46, "Intermediate Code & Code Generation","INTERMEDIATE", "Three-address code and target code generation.", 3),
    (230,46, "Code Optimization",            "ADVANCED",     "Local/global optimizations and peephole techniques.", 3),
    # Subject 47: Distributed Systems
    (231,47, "Distributed Architectures",    "BEGINNER",     "Client-server, peer-to-peer and layered models.", 2),
    (232,47, "Communication & RPC",          "BEGINNER",     "Sockets, remote procedure calls and message passing.", 2),
    (233,47, "Consistency & Replication",    "INTERMEDIATE", "CAP theorem, consistency models and replication strategies.", 3),
    (234,47, "Fault Tolerance & Consensus",  "INTERMEDIATE", "Failure detection, elections and Paxos/Raft.", 3),
    (235,47, "Distributed Transactions",     "ADVANCED",     "Two-phase commit, concurrency control and ordering.", 3),
    # Subject 48: Software Testing & Quality Assurance
    (236,48, "Testing Fundamentals",         "BEGINNER",     "Test levels, types and the V-model.", 2),
    (237,48, "Test Design Techniques",       "BEGINNER",     "Equivalence partitioning, boundary value and decision tables.", 2),
    (238,48, "Test Automation & Tools",      "INTERMEDIATE", "Selenium, JUnit and automation frameworks.", 3),
    (239,48, "CI/CD & Quality Metrics",      "INTERMEDIATE", "Pipelines, coverage, defect density and KPIs.", 3),
    (240,48, "Defect Management & Reporting", "ADVANCED",    "Severity/priority, lifecycle and traceability.", 3),
    # Subject 49: Data Warehousing & Data Mining
    (241,49, "Warehouse Architecture",       "BEGINNER",     "OLTP vs OLAP, star/snowflake schemas and layers.", 2),
    (242,49, "ETL & Data Integration",       "BEGINNER",     "Extraction, transformation, loading and cleansing.", 2),
    (243,49, "OLAP & Multidimensional Model","INTERMEDIATE", "Cubes, roll-up, drill-down and slicing/dicing.", 3),
    (244,49, "Data Mining Techniques",       "INTERMEDIATE", "Classification, regression and prediction workflows.", 3),
    (245,49, "Association & Clustering",     "ADVANCED",     "Apriori, FP-growth and k-means analysis.", 3),
    # Subject 50: Computer Graphics
    (246,50, "Graphics Primitives & Output", "BEGINNER",     "Points, lines, circles and scan conversion.", 2),
    (247,50, "2D Transformations",           "BEGINNER",     "Translation, rotation, scaling and homogeneous coordinates.", 2),
    (248,50, "3D Transformations & Projections","INTERMEDIATE", "3D transforms, viewing and perspective projection.", 3),
    (249,50, "Shading & Illumination",       "INTERMEDIATE", "Lighting models, Gouraud/Phong shading and textures.", 3),
    (250,50, "Rendering & Animation",        "ADVANCED",     "Hidden surface removal, ray tracing and keyframe animation.", 3),
    # Subject 51: Big Data Analytics
    (251,51, "Big Data Fundamentals",        "BEGINNER",     "Volume, velocity, variety and big data challenges.", 2),
    (252,51, "Hadoop & HDFS",                "BEGINNER",     "Hadoop ecosystem, HDFS architecture and YARN.", 2),
    (253,51, "MapReduce & Spark",            "INTERMEDIATE", "Map/Reduce jobs, RDDs and Spark pipelines.", 3),
    (254,51, "Streaming & NoSQL",            "INTERMEDIATE", "Kafka, real-time processing and NoSQL stores.", 3),
    (255,51, "Analytics & Visualization",    "ADVANCED",     "Dashboards, reporting and insight generation.", 3),
    # Subject 52: Blockchain & Cryptocurrency
    (256,52, "Blockchain Fundamentals",      "BEGINNER",     "Blocks, hashing, chains and distributed ledgers.", 2),
    (257,52, "Cryptography & Hashing",       "BEGINNER",     "Public/private keys, digital signatures and hashing.", 2),
    (258,52, "Consensus Mechanisms",         "INTERMEDIATE", "PoW, PoS, PBFT and finality.", 3),
    (259,52, "Smart Contracts & Platforms",  "INTERMEDIATE", "Solidity basics, Ethereum and contract deployment.", 3),
    (260,52, "Cryptocurrency & DApps",       "ADVANCED",     "Tokens, wallets, DeFi and decentralized applications.", 3),
    # Subject 53: Cyber Forensics & Incident Response
    (261,53, "Digital Evidence & Chain of Custody","BEGINNER", "Evidence types, collection and legal handling.", 2),
    (262,53, "Disk & File Forensics",        "BEGINNER",     "Imaging, filesystems and recovery of deleted data.", 2),
    (263,53, "Network & Memory Forensics",   "INTERMEDIATE", "Packet capture, log analysis and RAM acquisition.", 3),
    (264,53, "Forensic Tools & Analysis",    "INTERMEDIATE", "EnCase, Autopsy, Volatility and reporting.", 3),
    (265,53, "Incident Response & Reporting", "ADVANCED",    "Detection, containment, eradication and legal reports.", 3),
    # Subject 54: Project Work - Phase I
    (266,54, "Problem Identification",       "BEGINNER",     "Defining scope, objectives and success criteria.", 2),
    (267,54, "Literature Survey",            "BEGINNER",     "Related work, references and gap analysis.", 2),
    (268,54, "Requirement Analysis",         "INTERMEDIATE", "Functional/non-functional requirements and use cases.", 3),
    (269,54, "System Design & Architecture", "INTERMEDIATE", "High-level design, diagrams and tech stack selection.", 3),
    (270,54, "Project Documentation",        "ADVANCED",     "SRS, planning, schedules and progress reports.", 3),
    # Subject 55: Advanced Machine Learning
    (271,55, "Ensemble Methods",             "BEGINNER",     "Bagging, boosting, stacking and random forests.", 2),
    (272,55, "Deep Neural Networks",         "BEGINNER",     "Architecture, backpropagation and training tricks.", 2),
    (273,55, "CNN & RNN Architectures",      "INTERMEDIATE", "Convolutions, pooling, sequence models and LSTM.", 3),
    (274,55, "Reinforcement Learning",       "INTERMEDIATE", "MDPs, Q-learning, policy gradients and DQN.", 3),
    (275,55, "Transformers & Deployment",    "ADVANCED",     "Attention, BERT/GPT and MLOps deployment.", 3),
    # Subject 56: Capstone Project
    (276,56, "Project Planning & Scope",     "BEGINNER",     "Milestones, tasks and resource planning.", 2),
    (277,56, "Implementation & Development", "BEGINNER",     "Coding, version control and iterative builds.", 2),
    (278,56, "Testing & Quality Assurance",  "INTERMEDIATE", "Unit/integration/system testing and validation.", 3),
    (279,56, "Deployment & Demo",            "INTERMEDIATE", "Packaging, deployment and final demonstration.", 3),
    (280,56, "Defense & Report",             "ADVANCED",     "Viva, presentation and final project report.", 3),
    # Subject 57: Professional Ethics & IPR
    (281,57, "Professional Ethics",          "BEGINNER",     "Codes of conduct and ethical responsibilities.", 2),
    (282,57, "Intellectual Property Rights", "BEGINNER",     "Patents, copyrights, trademarks and trade secrets.", 2),
    (283,57, "Cyber Law & Data Privacy",     "INTERMEDIATE", "IT acts, GDPR and privacy frameworks.", 3),
    (284,57, "Corporate Responsibility",     "INTERMEDIATE", "Governance, sustainability and accountability.", 3),
    (285,57, "Case Studies & Codes",         "ADVANCED",     "Real-world dilemmas and ethical analysis.", 3),
    # Subject 58: Cloud Security & Governance
    (286,58, "Shared Responsibility Model",  "BEGINNER",     "Provider vs customer security duties.", 2),
    (287,58, "Identity & Access Management", "BEGINNER",     "IAM, roles, policies and SSO/MFA.", 2),
    (288,58, "Compliance & Auditing",        "INTERMEDIATE", "Standards, controls and audit trails.", 3),
    (289,58, "Zero Trust & Network Security","INTERMEDIATE", "Zero trust architecture and micro-segmentation.", 3),
    (290,58, "Cloud Incident & Forensics",   "ADVANCED",     "Detection, containment and cloud forensics.", 3),
    # Subject 59: Advanced Web Technologies
    (291,59, "Microservices & Containerization","BEGINNER", "Service decomposition, Docker and orchestration.", 2),
    (292,59, "GraphQL & API Design",         "BEGINNER",     "Schemas, queries, mutations and best practices.", 2),
    (293,59, "WebSockets & Real-time",       "INTERMEDIATE", "Full-duplex communication and event-driven web.", 3),
    (294,59, "Progressive Web Apps",         "INTERMEDIATE", "Service workers, offline support and installability.", 3),
    (295,59, "Edge Computing & Deployment",  "ADVANCED",     "Edge/CDN deployment, caching and performance.", 3),
    # Subject 60: Emerging Research in Computing
    (296,60, "Quantum Computing Basics",     "BEGINNER",     "Qubits, gates and quantum algorithms.", 2),
    (297,60, "Edge AI & On-device Learning", "BEGINNER",     "TinyML and inference at the edge.", 2),
    (298,60, "Metaverse & AR/VR",            "INTERMEDIATE", "Immersive platforms and mixed reality.", 3),
    (299,60, "Green Computing & Sustainability","INTERMEDIATE", "Energy efficiency and sustainable IT.", 3),
    (300,60, "Research Methodology & Trends","ADVANCED",     "Papers, experimentation and open problems.", 3),
    # Subject 61: Cell Biology & Biochemistry
    (301,61, "Cell Structure & Organization",  "BEGINNER",     "Cell theory, prokaryotes, plant and animal cell organelles.", 2),
    (302,61, "Biomolecules & Their Structure", "BEGINNER",     "Carbohydrates, proteins, lipids, nucleic acids and enzymes.", 2),
    (303,61, "Cell Membrane & Transport",       "INTERMEDIATE", "Fluid mosaic model, diffusion, osmosis, active transport.", 3),
    (304,61, "Enzymes & Metabolic Pathways",     "INTERMEDIATE", "Enzyme kinetics, inhibition, ATP and glycolysis.", 3),
    (305,61, "Photosynthesis & Respiration",     "ADVANCED",     "Light and dark reactions, Krebs cycle and oxidative phosphorylation.", 3),
    # Subject 62: Human Anatomy & Physiology
    (306,62, "Tissues & Organ Systems",         "BEGINNER",     "Epithelial, connective, muscular and nervous tissue, and organ systems.", 2),
    (307,62, "Skeletal & Muscular System",       "BEGINNER",     "Bones, joints, axial and appendicular skeleton, and muscle types.", 2),
    (308,62, "Cardiovascular & Respiratory Systems","INTERMEDIATE","Heart, blood vessels, blood and gas exchange in the lungs.", 3),
    (309,62, "Digestion, Excretion & Homeostasis","INTERMEDIATE","Digestive enzymes, kidney function, osmoregulation and balance.", 3),
    (310,62, "Nervous System & Sense Organs",    "ADVANCED",     "Neuron structure, CNS/PNS, reflexes and eye-ear sense organs.", 3),
    # Subject 63: Genetics & Molecular Biology
    (311,63, "DNA Structure & Replication",      "BEGINNER",     "Nucleotide structure, double helix, semiconservative replication.", 2),
    (312,63, "Mendelian Genetics & Inheritance", "INTERMEDIATE", "Monohybrid and dihybrid crosses, dominance and pedigree analysis.", 3),
    (313,63, "Transcription & Translation",     "INTERMEDIATE", "RNA polymerase, mRNA processing, ribosomes and the genetic code.", 3),
    (314,63, "Gene Regulation & Expression",     "ADVANCED",     "Operons, enhancers, epigenetic control and post-transcriptional control.", 3),
    (315,63, "Genetic Engineering & Biotechnology","ADVANCED",   "Restriction enzymes, vectors, PCR, cloning and CRISPR.", 3),
    # Subject 64: Ecology & Environmental Biology
    (316,64, "Ecosystem Structure & Function",  "BEGINNER",     "Biotic and abiotic factors, productivity and succession.", 2),
    (317,64, "Energy Flow & Food Chains",       "BEGINNER",     "Producers, consumers, decomposers, food webs and energy loss.", 2),
    (318,64, "Population Dynamics",             "INTERMEDIATE", "Growth curves, birth and death rates, carrying capacity.", 3),
    (319,64, "Biomes & Biodiversity",           "INTERMEDIATE", "Terrestrial and aquatic biomes, species diversity and hotspots.", 3),
    (320,64, "Pollution, Conservation & Sustainability","ADVANCED","Pollution types, conservation laws, afforestation and ecosystem health.", 3),
    # Subject 65: Microbiology & Biotechnology
    (321,65, "Microbial Diversity & Classification","BEGINNER", "Bacteria, archaea, fungi, protozoa, algae and virus classification.", 2),
    (322,65, "Bacterial Growth & Culture",      "BEGINNER",     "Growth curve, media, sterilization and culture techniques.", 2),
    (323,65, "Microbial Diseases & Immunity",   "INTERMEDIATE", "Pathogenesis, vaccines, antibodies and host defence.", 3),
    (324,65, "Recombinant DNA Technology",      "ADVANCED",     "Cloning vectors, gene libraries, transgenics and gene therapy.", 3),
    (325,65, "Industrial & Environmental Biotechnology","ADVANCED","Fermentation, biofuels, bioremediation and biofertilizers.", 3),
    # Subject 66: Financial Accounting
    (326,66, "Accounting Principles & Concepts", "BEGINNER",     "Accounting equation, accrual basis, GAAP and accounting elements.", 2),
    (327,66, "Journal & Ledger Entries",         "BEGINNER",     "Debit and credit rules, journal entries and posting to ledgers.", 2),
    (328,66, "Trial Balance & Rectification of Errors","INTERMEDIATE","Trial balance, suspense account and types of errors.", 3),
    (329,66, "Inventory & Valuation of Receivables","INTERMEDIATE","Valuation of inventory and receivables under FIFO, LIFO and weighted average.", 3),
    (330,66, "Final Accounts & Financial Statements","ADVANCED",  "Trading and profit and loss accounts, balance sheet and cash flow.", 3),
    # Subject 67: Business Economics & Management
    (331,67, "Nature & Forms of Business Organisation","BEGINNER","Sole proprietorship, partnership, company and cooperative forms.", 2),
    (332,67, "Management Functions & Planning",  "BEGINNER",     "Planning, organizing, directing and controlling functions.", 2),
    (333,67, "Organisational Structure & Delegation","INTERMEDIATE","Line and staff authority, span of control, centralization and decentralization.", 3),
    (334,67, "Leadership, Motivation & Control", "INTERMEDIATE", "Leadership styles, motivation theories and performance control.", 3),
    (335,67, "Business Environment & Decision Making","ADVANCED", "Economic environment, SWOT, forecasting and decision-making techniques.", 3),
    # Subject 68: Cost Accounting & Budgeting
    (336,68, "Cost Concepts & Cost Behaviour",   "BEGINNER",     "Fixed, variable, semi-variable costs and cost behaviour analysis.", 2),
    (337,68, "Cost Sheet & Cost Classification", "INTERMEDIATE", "Prime, conversion and period costs, and element and function classification.", 3),
    (338,68, "Variance Analysis",               "INTERMEDIATE", "Material, labour, overhead and sales variances with causes.", 3),
    (339,68, "Standard Costing & Marginal Costing","ADVANCED",  "Setting standards, reconciliation and break-even analysis.", 3),
    (340,68, "Budgeting & Responsibility Accounting","ADVANCED","Master budgets, cash budgets, responsibility centres and performance reports.", 3),
    # Subject 69: Marketing & Sales Management
    (341,69, "Marketing Concepts & Consumer Behaviour","BEGINNER","Needs, wants, demand, consumer types and buying behaviour.", 2),
    (342,69, "Product, Brand & Packaging",       "BEGINNER",     "Product levels, brand equity, packaging and labelling.", 2),
    (343,69, "Pricing & Distribution Channels", "INTERMEDIATE", "Pricing methods, price skimming, channels and retailing.", 3),
    (344,69, "Promotion, Advertising & Digital Marketing","INTERMEDIATE","Promotion mix, advertising media, sales promotion and digital marketing.", 3),
    (345,69, "Sales Management & CRM",          "ADVANCED",     "Sales forecasting, territories, sales force management and CRM.", 3),
    # Subject 70: Banking, Taxation & Corporate Law
    (346,70, "Banking Principles & Functions",   "BEGINNER",     "Central bank, commercial banks, RBI functions and banking principles.", 2),
    (347,70, "Banking Instruments & Negotiable Instruments","INTERMEDIATE","Cheques, bills, promissory notes, deposits, loans and credit cards.", 3),
    (348,70, "Types of Accounts & Banking Regulation","INTERMEDIATE","Deposits, loans, overdraft, NPA management and prudential norms.", 3),
    (349,70, "Direct & Indirect Taxation",      "ADVANCED",     "Income tax, GST, customs duty, tax planning and filing.", 3),
    (350,70, "Corporate Law, Contracts & Consumer Protection","ADVANCED","Companies Act, contracts Act, consumer rights and company offences.", 3),
]

# ---- helper lookups ---------------------------------------------------------
def subject_by_id(sid):
    for s in SUBJECTS:
        if s[0] == sid:
            return s
    raise KeyError(sid)

def category_by_id(cid):
    for c in CATEGORIES:
        if c[0] == cid:
            return c
    raise KeyError(cid)

def topics_of_subject(sid):
    return [t for t in TOPICS if t[1] == sid]

def subjects_of_category(cid):
    return [s for s in SUBJECTS if s[1] == cid]

def topics_of_category(cid):
    out = []
    for s in subjects_of_category(cid):
        out.extend(topics_of_subject(s[0]))
    return out

def difficulty_order():
    return ["BEGINNER", "INTERMEDIATE", "ADVANCED"]

def marks_for(difficulty):
    return { "BEGINNER": 1, "INTERMEDIATE": 2, "ADVANCED": 3 }[difficulty]

if __name__ == "__main__":
    print("categories:", len(CATEGORIES))
    print("subjects:  ", len(SUBJECTS))
    print("topics:    ", len(TOPICS))
    for cid, cname, _ in CATEGORIES:
        print(f"  cat {cid}: {cname} -> {len(subjects_of_category(cid))} subjects, {len(topics_of_category(cid))} topics")
