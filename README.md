# LLM-Guided Web Scraper Repair System

A Kotlin-based proof of concept for dynamic web scraper repair using locally deployed LLMs

## 📌 Overview

This repository contains the source code for a modular system that autonomously repairs failing web scrapers at runtime. By combining prompt engineering techniques with locally hosted large language models (LLMs), the system identifies updated DOM elements and regenerates the required scraper code on the fly.

This approach aims to reduce the need for manual maintenance of RPA (Robotic Process Automation) scripts by enabling:

Semantic element matching when selectors fail
Code regeneration using code-oriented LLMs
Modular deployment and runtime compilation
Local LLM hosting via Ollama

## 🧠 Key Features

Snapshot-Based Failure Recovery
Capture and compare past and current HTML snapshots to identify missing or updated elements.
LLM-Driven Correction Pipeline
Combines Few-Shot Prompting and Chain-of-Thought reasoning to locate and suggest replacements for failed selectors.
Runtime Code Generation & Loading
Generates updated Kotlin scraper code using CodeLlama, compiles it, and loads it into memory without restarting the app.
Local Execution with Ollama
All LLM inference is done locally via the Ollama platform to avoid external dependencies and ensure low-latency interactions.
