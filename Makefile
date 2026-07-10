# Smoothie Maker — use `make` or `make help` for targets

MVN          := mvn
ARTIFACT     := $(shell $(MVN) help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
VERSION      := $(shell $(MVN) help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR          := target/$(ARTIFACT)-$(VERSION).jar
MAIN_CLASS   := org.example.smoothies.SmoothieApp
DEBUG_PORT   ?= 8787
JVM_DEBUG    := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$(DEBUG_PORT)

# JDK 24+: silence sun.misc.Unsafe warnings from Spotless on newer local JDKs
export MAVEN_OPTS ?= --sun-misc-unsafe-memory-access=allow

.PHONY: help develop verify build workspace icons hooks \
        dev debug run jar prod \
        test compile format lint verify install \
        package build clean jar-path

.DEFAULT_GOAL := help

# --- Workspace ----------------------------------------------------------------

##@ Workspace
## icons: Generate app icons from assets/logo.png (ImageMagick)
icons:
	python3 scripts/generate_icons.py

## help: List targets
help:
	@printf '%s\n' 'Smoothie Maker'
	@awk '/^##@ / { printf "\n%s\n", substr($$0, 5); next } \
	     /^## [a-z]/ { line=$$0; sub(/^## /,"",line); split(line,a,": "); \
	       printf "  %-14s %s\n", a[1], a[2] }' $(MAKEFILE_LIST)
	@printf '\nExamples:\n  make dev     # Spring Boot\n  make jar     # packaged JAR\n  make verify  # tests + package\n'

## jar-path: Print path to the packaged JAR
jar-path:
	@echo $(JAR)

## clean: Remove build output
clean:
	$(MVN) clean

## hooks: Install git pre-commit hook (Spotless + compile)
hooks:
	./scripts/install-git-hooks.sh

# --- Develop ------------------------------------------------------------------

##@ Develop
## dev: Run via Spring Boot (dev profile); compile first so IDE-stale classes are not used
dev: compile
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=dev

## debug: Run with remote debugging on port $(DEBUG_PORT)
debug:
	$(MVN) spring-boot:run -Dspring-boot.run.profiles=dev \
		-Dspring-boot.run.jvmArguments="$(JVM_DEBUG)"

## run: Alias for dev
run: dev

## jar: Build and run the fat JAR
jar: package
	java -jar $(JAR)

## prod: Alias for jar
prod: jar

# --- Verify -------------------------------------------------------------------

##@ Verify
## test: Run unit tests
test:
	$(MVN) test

## compile: Compile main and test sources
compile:
	$(MVN) compile test-compile

## format: Apply code formatting (Spotless)
format:
	$(MVN) spotless:apply -q

## lint: Check formatting and compile
lint: format-check compile

.PHONY: format-check
format-check:
	$(MVN) spotless:check -q

## verify: Run tests and package
verify:
	$(MVN) verify

# --- Build --------------------------------------------------------------------

##@ Build
## package: Build executable JAR (skip tests)
package:
	$(MVN) package -DskipTests

## build: Alias for package
build: package

## install: Install to local Maven repository
install:
	$(MVN) install
