-- ============================================================================
-- AI-Powered Student Performance Prediction & Personalized Learning System
-- DELTA SEED - Biology & Commerce departments
--
-- Adds the two non-computing departments to an ALREADY SEEDED database:
--   category 9  : Biology & Life Sciences
--   category 10 : Commerce, Accounting & Business Studies
--   subjects 61-65 (department 'Biology')
--   subjects 66-70 (department 'Commerce')
--   topics   301-350
--
-- Safe to run more than once (idempotent). Mirrors the rows already present in
-- seed_base.sql, so a fresh install can simply run seed_base.sql instead.
--
-- Usage:
--   mysql -u root -p student_performance_db < database/seed_biology_commerce.sql
--
-- NOTE: topics are created without questions. Question data is authored in
-- scripts/question_data/subject_61.py .. subject_70.py and emitted by
-- scripts/generate_questions.py into seed_biology_questions.sql and
-- seed_commerce_questions.sql. Until then these subjects show 0 questions.
-- ============================================================================

USE student_performance_db;

-- ============================ CATEGORIES ====================================
INSERT INTO categories (category_id, category_name) VALUES
(9, 'Biology & Life Sciences'),
(10, 'Commerce, Accounting & Business Studies')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name);

-- ============================ SUBJECTS =====================================
INSERT INTO subjects (subject_id, category_id, subject_code, subject_name, department, credit_hours, description) VALUES
(61, 9, 'BIO101', 'Cell Biology & Biochemistry', 'Biology', 4.0, 'Cell structure, membranes, transport, enzymes, metabolism, photosynthesis and respiration.'),
(62, 9, 'BIO102', 'Human Anatomy & Physiology', 'Biology', 4.0, 'Tissues, organ systems, skeleton, muscles, circulation, respiration, digestion and nervous system.'),
(63, 9, 'BIO103', 'Genetics & Molecular Biology', 'Biology', 4.0, 'DNA replication, transcription, translation, Mendelian genetics, regulation and genetic engineering.'),
(64, 9, 'BIO104', 'Ecology & Environmental Biology', 'Biology', 3.0, 'Ecosystems, energy flow, food chains, population dynamics, biomes, biodiversity and conservation.'),
(65, 9, 'BIO105', 'Microbiology & Biotechnology', 'Biology', 4.0, 'Microbial diversity, bacterial growth, pathogens, immunity, recombinant DNA and industrial biotechnology.'),
(66, 10, 'COM101', 'Financial Accounting', 'Commerce', 4.0, 'Accounting principles, journals, ledgers, trial balance, error rectification, valuation and final accounts.'),
(67, 10, 'COM102', 'Business Economics & Management', 'Commerce', 3.0, 'Business organisation, management functions, planning, structure, leadership, control and decision making.'),
(68, 10, 'COM103', 'Cost Accounting & Budgeting', 'Commerce', 4.0, 'Cost concepts, cost sheets, classification, variance analysis, standard and marginal costing, budgeting.'),
(69, 10, 'COM104', 'Marketing & Sales Management', 'Commerce', 3.0, 'Marketing concepts, consumer behaviour, product and brand decisions, pricing, distribution, promotion and CRM.'),
(70, 10, 'COM105', 'Banking, Taxation & Corporate Law', 'Commerce', 4.0, 'Banking functions, instruments, accounts, regulation, direct and indirect tax, contracts and corporate law.')
ON DUPLICATE KEY UPDATE
  category_id  = VALUES(category_id),
  subject_name = VALUES(subject_name),
  department   = VALUES(department),
  credit_hours = VALUES(credit_hours),
  description  = VALUES(description),
  is_active    = 1;

-- ============================ TOPICS =======================================
INSERT INTO topics (topic_id, subject_id, topic_name, difficulty_level, description, estimated_hours) VALUES
(301, 61, 'Cell Structure & Organization', 'BEGINNER', 'Cell theory, prokaryotes, plant and animal cell organelles.', 2),
(302, 61, 'Biomolecules & Their Structure', 'BEGINNER', 'Carbohydrates, proteins, lipids, nucleic acids and enzymes.', 2),
(303, 61, 'Cell Membrane & Transport', 'INTERMEDIATE', 'Fluid mosaic model, diffusion, osmosis, active transport.', 3),
(304, 61, 'Enzymes & Metabolic Pathways', 'INTERMEDIATE', 'Enzyme kinetics, inhibition, ATP and glycolysis.', 3),
(305, 61, 'Photosynthesis & Respiration', 'ADVANCED', 'Light and dark reactions, Krebs cycle and oxidative phosphorylation.', 3),
(306, 62, 'Tissues & Organ Systems', 'BEGINNER', 'Epithelial, connective, muscular and nervous tissue, and organ systems.', 2),
(307, 62, 'Skeletal & Muscular System', 'BEGINNER', 'Bones, joints, axial and appendicular skeleton, and muscle types.', 2),
(308, 62, 'Cardiovascular & Respiratory Systems', 'INTERMEDIATE', 'Heart, blood vessels, blood and gas exchange in the lungs.', 3),
(309, 62, 'Digestion, Excretion & Homeostasis', 'INTERMEDIATE', 'Digestive enzymes, kidney function, osmoregulation and balance.', 3),
(310, 62, 'Nervous System & Sense Organs', 'ADVANCED', 'Neuron structure, CNS/PNS, reflexes and eye-ear sense organs.', 3),
(311, 63, 'DNA Structure & Replication', 'BEGINNER', 'Nucleotide structure, double helix, semiconservative replication.', 2),
(312, 63, 'Mendelian Genetics & Inheritance', 'INTERMEDIATE', 'Monohybrid and dihybrid crosses, dominance and pedigree analysis.', 3),
(313, 63, 'Transcription & Translation', 'INTERMEDIATE', 'RNA polymerase, mRNA processing, ribosomes and the genetic code.', 3),
(314, 63, 'Gene Regulation & Expression', 'ADVANCED', 'Operons, enhancers, epigenetic control and post-transcriptional control.', 3),
(315, 63, 'Genetic Engineering & Biotechnology', 'ADVANCED', 'Restriction enzymes, vectors, PCR, cloning and CRISPR.', 3),
(316, 64, 'Ecosystem Structure & Function', 'BEGINNER', 'Biotic and abiotic factors, productivity and succession.', 2),
(317, 64, 'Energy Flow & Food Chains', 'BEGINNER', 'Producers, consumers, decomposers, food webs and energy loss.', 2),
(318, 64, 'Population Dynamics', 'INTERMEDIATE', 'Growth curves, birth and death rates, carrying capacity.', 3),
(319, 64, 'Biomes & Biodiversity', 'INTERMEDIATE', 'Terrestrial and aquatic biomes, species diversity and hotspots.', 3),
(320, 64, 'Pollution, Conservation & Sustainability', 'ADVANCED', 'Pollution types, conservation laws, afforestation and ecosystem health.', 3),
(321, 65, 'Microbial Diversity & Classification', 'BEGINNER', 'Bacteria, archaea, fungi, protozoa, algae and virus classification.', 2),
(322, 65, 'Bacterial Growth & Culture', 'BEGINNER', 'Growth curve, media, sterilization and culture techniques.', 2),
(323, 65, 'Microbial Diseases & Immunity', 'INTERMEDIATE', 'Pathogenesis, vaccines, antibodies and host defence.', 3),
(324, 65, 'Recombinant DNA Technology', 'ADVANCED', 'Cloning vectors, gene libraries, transgenics and gene therapy.', 3),
(325, 65, 'Industrial & Environmental Biotechnology', 'ADVANCED', 'Fermentation, biofuels, bioremediation and biofertilizers.', 3),
(326, 66, 'Accounting Principles & Concepts', 'BEGINNER', 'Accounting equation, accrual basis, GAAP and accounting elements.', 2),
(327, 66, 'Journal & Ledger Entries', 'BEGINNER', 'Debit and credit rules, journal entries and posting to ledgers.', 2),
(328, 66, 'Trial Balance & Rectification of Errors', 'INTERMEDIATE', 'Trial balance, suspense account and types of errors.', 3),
(329, 66, 'Inventory & Valuation of Receivables', 'INTERMEDIATE', 'Valuation of inventory and receivables under FIFO, LIFO and weighted average.', 3),
(330, 66, 'Final Accounts & Financial Statements', 'ADVANCED', 'Trading and profit and loss accounts, balance sheet and cash flow.', 3),
(331, 67, 'Nature & Forms of Business Organisation', 'BEGINNER', 'Sole proprietorship, partnership, company and cooperative forms.', 2),
(332, 67, 'Management Functions & Planning', 'BEGINNER', 'Planning, organizing, directing and controlling functions.', 2),
(333, 67, 'Organisational Structure & Delegation', 'INTERMEDIATE', 'Line and staff authority, span of control, centralization and decentralization.', 3),
(334, 67, 'Leadership, Motivation & Control', 'INTERMEDIATE', 'Leadership styles, motivation theories and performance control.', 3),
(335, 67, 'Business Environment & Decision Making', 'ADVANCED', 'Economic environment, SWOT, forecasting and decision-making techniques.', 3),
(336, 68, 'Cost Concepts & Cost Behaviour', 'BEGINNER', 'Fixed, variable, semi-variable costs and cost behaviour analysis.', 2),
(337, 68, 'Cost Sheet & Cost Classification', 'INTERMEDIATE', 'Prime, conversion and period costs, and element and function classification.', 3),
(338, 68, 'Variance Analysis', 'INTERMEDIATE', 'Material, labour, overhead and sales variances with causes.', 3),
(339, 68, 'Standard Costing & Marginal Costing', 'ADVANCED', 'Setting standards, reconciliation and break-even analysis.', 3),
(340, 68, 'Budgeting & Responsibility Accounting', 'ADVANCED', 'Master budgets, cash budgets, responsibility centres and performance reports.', 3),
(341, 69, 'Marketing Concepts & Consumer Behaviour', 'BEGINNER', 'Needs, wants, demand, consumer types and buying behaviour.', 2),
(342, 69, 'Product, Brand & Packaging', 'BEGINNER', 'Product levels, brand equity, packaging and labelling.', 2),
(343, 69, 'Pricing & Distribution Channels', 'INTERMEDIATE', 'Pricing methods, price skimming, channels and retailing.', 3),
(344, 69, 'Promotion, Advertising & Digital Marketing', 'INTERMEDIATE', 'Promotion mix, advertising media, sales promotion and digital marketing.', 3),
(345, 69, 'Sales Management & CRM', 'ADVANCED', 'Sales forecasting, territories, sales force management and CRM.', 3),
(346, 70, 'Banking Principles & Functions', 'BEGINNER', 'Central bank, commercial banks, RBI functions and banking principles.', 2),
(347, 70, 'Banking Instruments & Negotiable Instruments', 'INTERMEDIATE', 'Cheques, bills, promissory notes, deposits, loans and credit cards.', 3),
(348, 70, 'Types of Accounts & Banking Regulation', 'INTERMEDIATE', 'Deposits, loans, overdraft, NPA management and prudential norms.', 3),
(349, 70, 'Direct & Indirect Taxation', 'ADVANCED', 'Income tax, GST, customs duty, tax planning and filing.', 3),
(350, 70, 'Corporate Law, Contracts & Consumer Protection', 'ADVANCED', 'Companies Act, contracts Act, consumer rights and company offences.', 3)
ON DUPLICATE KEY UPDATE
  difficulty_level = VALUES(difficulty_level),
  description      = VALUES(description),
  estimated_hours  = VALUES(estimated_hours);

-- ============================ VERIFICATION ==================================
SELECT category_id, category_name FROM categories WHERE category_id IN (9, 10);

SELECT department, COUNT(*) AS subjects
FROM subjects
WHERE department IN ('Biology', 'Commerce')
GROUP BY department;

SELECT s.department, COUNT(t.topic_id) AS topics
FROM subjects s
JOIN topics t ON t.subject_id = s.subject_id
WHERE s.department IN ('Biology', 'Commerce')
GROUP BY s.department;
