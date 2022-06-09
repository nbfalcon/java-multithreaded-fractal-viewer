from matplotlib import pyplot as plt


def main():
    cm = plt.get_cmap('coolwarm')
    # noinspection PyProtectedMember
    cm._init()
    print(cm(0.5))


if __name__ == '__main__':
    main()
