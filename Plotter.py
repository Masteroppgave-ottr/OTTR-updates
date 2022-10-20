
import matplotlib.pyplot as plt


def find_all(measurement_list, solution):
    matches = []
    for i in measurement_list:
        if i[1] == solution:
            matches.append(i)

    return matches


def get_n(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(i[0])
    return matches


def get_time(measurement_list):
    matches = []
    for i in measurement_list:
        matches.append(i[2])
    return matches


def find_interval(measurement_list, solution, start, end):
    matches = []
    for i in measurement_list:
        if i[1] == solution and i[2] == start:
            # find the next measurement with the same solution and end
            for j in measurement_list:
                if j[0] == i[0] and j[1] == solution and j[2] == end:
                    matches.append([i[0], i[1], j[2], int(j[3]) - int(i[3])])
                    break

    return matches


measurements = []
with open("temp/times.txt", "r") as f:
    lines = f.readlines()

    for line in lines:
        line = line.strip()
        args = line.split(" ; ")
        measurements.append(args)
        # [n, solution, tag, time] = args

interval = find_interval(measurements, "naive solution", "start", "end")
n = get_n(interval)
time = get_time(interval)

print(interval)
print(n)
print(time)


plt.plot(n, time)
plt.show()
