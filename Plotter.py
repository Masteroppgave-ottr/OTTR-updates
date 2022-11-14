
from ast import arg
from cProfile import label
import sys
import matplotlib.pyplot as plt
import numpy as np

# static variables
instances_i = 0
changes_i = 1
solution_i = 2
tag_i = 3
time_i = 4


def find_all_solutions(measurement_list):
    solutions = []
    for i in measurement_list:
        if i[solution_i] not in solutions:
            solutions.append(i[solution_i])
    return solutions


def get_instances(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[instances_i]))
    return matches


def get_changes(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[changes_i]))
    return matches


def get_time(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(int(i[time_i]))
    return matches


def find_interval(measurement_list, solution, start_tag, end_tag, field=instances_i):
    """
    For a given solution, find the interval between `start` and `end` for every n
    """
    matches = []
    for i in measurement_list:
        if i[solution_i] == solution and i[tag_i] == start_tag:
            # find the next measurement with the same solution and end
            for j in measurement_list:
                if j[field] == i[field] and j[solution_i] == solution and j[tag_i] == end_tag:
                    matches.append([i[instances_i], i[changes_i], i[solution_i],
                                   j[tag_i], int(j[time_i]) - int(i[time_i])])
                    break

    return matches


def get_instance_change_time_lists(measurement_list, solution, start, end, field=instances_i):
    """find n and time for measurements for the given solution between `start` and `end`
    """
    interval = find_interval(measurement_list, solution, start, end, field)
    n = get_instances(interval)
    changes = get_changes(interval)
    time = get_time(interval)
    return n, changes, time


def read_file(filename):
    """
    read the content of the file, and parse to list
    Format of file content: ``` n, solution, label, time ```
    Example: ```10 ; naive solution ; start ; 34328947247251```
    """
    measurements = []
    with open(filename, "r") as f:
        lines = f.readlines()

        for line in lines:
            line = line.strip()
            args = line.split(" ; ")
            measurements.append(args)
    return measurements


def create_bar_interval(measurement_list, field=instances_i):
    solutions = find_all_solutions(measurement_list)
    width = 0.2
    x = np.arange(3)
    counter = 0
    instances = [-1]
    changes = [-1]
    for solution in solutions:
        counter += 1
        solutionTimes = []
        instances, changes, diffTime = get_instance_change_time_lists(
            measurement_list, solution, "start", "diff", field)
        instances, changes, modelTime = get_instance_change_time_lists(
            measurement_list, solution, "diff", "model", field)
        instances, changes, queryTime = get_instance_change_time_lists(
            measurement_list, solution, "model", "end", field)

        solutionTimes.append(diffTime[0])
        solutionTimes.append(modelTime[0])
        solutionTimes.append(queryTime[0])

        if (len(solutions) == 1):
            plt.bar(x, solutionTimes, width=width, label=solution)
        elif (counter % 2 == 0):
            plt.bar(x+((width/2)*(counter-1)),
                    solutionTimes, width, label=solution)
        else:
            plt.bar(x-((width/2)*counter),
                    solutionTimes, width, label=solution)

    plt.xticks(x, ["diff", "expand instances", "query"])
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(f"Instances = {instances[0]} | Changes = {changes[0]}")
    print("[PLOT] Creating bar chart")
    plt.savefig("./temp/bar.png")


def create_line_graph_nInstances(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        n, changes, time = get_instance_change_time_lists(
            measurement_list, solution, "start", "end", instances_i)

        plt.plot(n, time, label=solution)

    plt.xlabel("number of instances")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of changes = {changes[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def create_line_graph_nChanges(measurement_list):
    solutions = find_all_solutions(measurement_list)
    for solution in solutions:
        instances, changes, time = get_instance_change_time_lists(
            measurement_list, solution, "start", "end", changes_i)

        plt.plot(changes, time, label=solution)

    plt.xlabel("number of changes")
    plt.ylabel("Time in nano seconds")
    plt.legend(solutions)
    plt.title(
        f"Runtime | Number of Instances = {instances[0]}")
    print("[PLOT] Creating line graph")
    plt.savefig("./temp/line.png")


def has_multiple_n(measurement_list, field=instances_i):
    first_n = measurement_list[0][field]
    for measurement in measurement_list:
        if (measurement[field] != first_n):
            return True
    return False


if __name__ == '__main__':
    # get first command line argument
    plot_type = sys.argv[1]
    all_measurements = read_file(sys.argv[2])

    if plot_type == "n=instances":
        if has_multiple_n(all_measurements, instances_i):
            create_line_graph_nInstances(all_measurements)
        else:
            create_bar_interval(all_measurements, instances_i)

    elif plot_type == "n=changes":
        if has_multiple_n(all_measurements, changes_i):
            create_line_graph_nChanges(all_measurements)
        else:
            create_bar_interval(all_measurements, changes_i)

    else:
        print(f"Unknown plot type '{plot_type}'")
        sys.exit(1)
